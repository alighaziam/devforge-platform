package io.devforge.platform.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.devforge.platform.core.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class DomainModelDiffService {
    private static final int MAX_COMBINED_BYTES = 524_288;

    private final ProjectRepository projects;

    public DomainModelDiffService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public ModelDiffResult diff(UUID projectId, JsonNode before, JsonNode after) {
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        validateModel(before, "before");
        validateModel(after, "after");
        int size = before.toString().getBytes(StandardCharsets.UTF_8).length
                + after.toString().getBytes(StandardCharsets.UTF_8).length;
        if (size > MAX_COMBINED_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Combined model snapshots exceed the 512 KiB limit");
        }

        List<ModelDiffResult.ModelChange> changes = new ArrayList<>();
        compareEntities(before.path("entities"), after.path("entities"), changes);
        compareCollection("relationships", before.path("relationships"), after.path("relationships"),
                "name", changes);
        compareIndexes(before.path("indexes"), after.path("indexes"), changes);
        changes.sort(java.util.Comparator.comparing(ModelDiffResult.ModelChange::path)
                .thenComparing(ModelDiffResult.ModelChange::changeType));
        return new ModelDiffResult(changes);
    }

    private void validateModel(JsonNode model, String label) {
        if (model == null || !model.isObject() || !model.path("entities").isArray()) {
            throw invalid(label + " model must be an object containing an entities array");
        }
        for (String collection : List.of("relationships", "indexes")) {
            if (model.has(collection) && !model.path(collection).isArray()) {
                throw invalid(label + " model " + collection + " must be an array");
            }
        }
        for (JsonNode entity : model.path("entities")) {
            if (!entity.isObject() || entity.path("name").asText("").isBlank()) {
                throw invalid(label + " model entities must be objects with a non-empty name");
            }
            if (entity.has("attributes") && !entity.path("attributes").isArray()) {
                throw invalid(label + " entity attributes must be an array");
            }
            for (JsonNode attribute : entity.path("attributes")) {
                if (!attribute.isObject() || attribute.path("name").asText("").isBlank()) {
                    throw invalid(label + " model attributes must be objects with a non-empty name");
                }
            }
        }
    }

    private void compareEntities(JsonNode before, JsonNode after,
                                 List<ModelDiffResult.ModelChange> changes) {
        Map<String, JsonNode> oldEntities = index(before, "name", "entities");
        Map<String, JsonNode> newEntities = index(after, "name", "entities");
        for (String name : union(oldEntities, newEntities)) {
            String path = "/entities/" + pointer(name);
            JsonNode oldEntity = oldEntities.get(name);
            JsonNode newEntity = newEntities.get(name);
            if (oldEntity == null) {
                add(changes, path, "added", null, newEntity);
            } else if (newEntity == null) {
                add(changes, path, "removed", oldEntity, null);
            } else {
                ObjectNode oldFields = ((ObjectNode) oldEntity).deepCopy();
                ObjectNode newFields = ((ObjectNode) newEntity).deepCopy();
                oldFields.remove("attributes");
                newFields.remove("attributes");
                if (!oldFields.equals(newFields)) {
                    add(changes, path, "modified", oldFields, newFields);
                }
                compareCollection(path + "/attributes", oldEntity.path("attributes"),
                        newEntity.path("attributes"), "name", changes);
            }
        }
    }

    private void compareIndexes(JsonNode before, JsonNode after,
                                List<ModelDiffResult.ModelChange> changes) {
        Map<String, JsonNode> oldIndexes = index(before, null, "indexes");
        Map<String, JsonNode> newIndexes = index(after, null, "indexes");
        compareIndexed("/indexes", oldIndexes, newIndexes, changes);
    }

    private void compareCollection(String path, JsonNode before, JsonNode after, String key,
                                   List<ModelDiffResult.ModelChange> changes) {
        Map<String, JsonNode> oldItems = index(before, key, path);
        Map<String, JsonNode> newItems = index(after, key, path);
        compareIndexed(path, oldItems, newItems, changes);
    }

    private void compareIndexed(String path, Map<String, JsonNode> before, Map<String, JsonNode> after,
                                List<ModelDiffResult.ModelChange> changes) {
        for (String key : union(before, after)) {
            JsonNode oldValue = before.get(key);
            JsonNode newValue = after.get(key);
            String itemPath = path + "/" + pointer(key);
            if (oldValue == null) {
                add(changes, itemPath, "added", null, newValue);
            } else if (newValue == null) {
                add(changes, itemPath, "removed", oldValue, null);
            } else if (!oldValue.equals(newValue)) {
                add(changes, itemPath, "modified", oldValue, newValue);
            }
        }
    }

    private Map<String, JsonNode> index(JsonNode values, String keyField, String collection) {
        Map<String, JsonNode> indexed = new TreeMap<>();
        if (!values.isArray()) return indexed;
        for (JsonNode value : values) {
            if (!value.isObject()) {
                throw invalid(collection + " entries must be JSON objects");
            }
            String key = keyField == null ? identity(value, collection) : value.path(keyField).asText("");
            if (key.isBlank()) {
                throw invalid(collection + " entries must have a stable identity");
            }
            if (indexed.putIfAbsent(key, value) != null) {
                throw invalid(collection + " contains duplicate identity: " + key);
            }
        }
        return indexed;
    }

    private String identity(JsonNode value, String collection) {
        String name = value.path("name").asText("");
        if (!name.isBlank()) return name;
        if ("indexes".equals(collection)) {
            return value.path("entity").asText("") + ":"
                    + value.path("columns").toString();
        }
        return value.path("from").asText("") + "->" + value.path("to").asText("")
                + ":" + value.path("cardinality").asText("");
    }

    private List<String> union(Map<String, JsonNode> before, Map<String, JsonNode> after) {
        java.util.TreeSet<String> keys = new java.util.TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        return List.copyOf(keys);
    }

    private void add(List<ModelDiffResult.ModelChange> changes, String path, String type,
                     JsonNode before, JsonNode after) {
        changes.add(new ModelDiffResult.ModelChange(path, type,
                before == null ? null : before.deepCopy(),
                after == null ? null : after.deepCopy()));
    }

    private String pointer(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
