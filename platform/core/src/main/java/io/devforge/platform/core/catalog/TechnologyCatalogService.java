package io.devforge.platform.core.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TechnologyCatalogService {

    private static final String INDEX_RESOURCE = "catalog-index.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Map<String, Object>> technologies;
    private final List<Map<String, Object>> compatibilityRules;

    public TechnologyCatalogService() {
        JsonNode index = readJson(INDEX_RESOURCE);
        this.technologies = loadEntries(index.path("technologies"));
        this.compatibilityRules = loadEntries(index.path("compatibilityRules"));
    }

    public List<Map<String, Object>> findTechnologies(String category) {
        if (category == null || category.isBlank()) {
            return technologies;
        }
        return technologies.stream()
                .filter(entry -> category.equals(entry.get("category")))
                .toList();
    }

    public List<Map<String, Object>> findCompatibilityRules() {
        return compatibilityRules;
    }

    public Map<String, Object> findTechnology(String id) {
        return technologies.stream()
                .filter(entry -> Objects.equals(id, entry.get("id")))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> loadEntries(JsonNode paths) {
        if (!paths.isArray()) {
            throw new IllegalStateException("Catalog index entries must be arrays");
        }
        return java.util.stream.StreamSupport.stream(paths.spliterator(), false)
                .map(JsonNode::asText)
                .map(this::readJson)
                .map(node -> objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() { }))
                .toList();
    }

    private JsonNode readJson(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new CatalogResourceException("Unable to load catalog resource: " + path, exception);
        }
    }
}
