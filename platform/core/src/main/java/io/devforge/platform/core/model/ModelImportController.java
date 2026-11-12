package io.devforge.platform.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/model")
public class ModelImportController {
    private final ModelImportService importer;

    public ModelImportController(ModelImportService importer) {
        this.importer = importer;
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ModelImportResult importModel(@PathVariable UUID projectId,
                                         @RequestBody ModelImportRequest request)
            throws JsonProcessingException {
        return importer.importModel(projectId, request.format(), request.content());
    }

    public record ModelImportRequest(String format, String content) { }
}
