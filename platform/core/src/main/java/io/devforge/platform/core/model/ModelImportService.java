package io.devforge.platform.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelImportService {
    private final ProjectRepository projects;
    private final SqlDdlModelImporter sqlImporter;
    private final ObjectMapper mapper = new ObjectMapper();

    public ModelImportService(ProjectRepository projects, SqlDdlModelImporter sqlImporter) {
        this.projects = projects;
        this.sqlImporter = sqlImporter;
    }

    @Transactional(readOnly = true)
    public ModelImportResult importModel(UUID projectId, String format, String content)
            throws JsonProcessingException {
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (content == null || content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 524_288) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Model source exceeds the 512 KiB limit");
        }
        Map<String, Object> model;
        if ("sql-ddl".equalsIgnoreCase(format)) {
            model = sqlImporter.importDdl(content);
        } else if ("json".equalsIgnoreCase(format)) {
            JsonNode root = mapper.readTree(content);
            if (!root.isObject() || !root.path("entities").isArray()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Model JSON must be an object containing an entities array");
            }
            if (root.has("relationships") && !root.path("relationships").isArray()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Model relationships must be an array");
            }
            if (root.has("indexes") && !root.path("indexes").isArray()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Model indexes must be an array");
            }
            model = mapper.convertValue(root, Map.class);
            model.putIfAbsent("relationships", List.of());
            model.putIfAbsent("indexes", List.of());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported model import format. Supported formats: json, sql-ddl");
        }
        return new ModelImportResult(model, List.of(
                "Import is a preview only; review and merge the model into the Project Specification before saving.",
                "Foreign-key relationships, composite primary keys, and SQL defaults are not supported by this importer."));
    }
}
