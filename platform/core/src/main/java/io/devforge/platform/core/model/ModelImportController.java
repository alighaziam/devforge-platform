package io.devforge.platform.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/model")
public class ModelImportController {
    private final ModelImportService importer;
    private final DomainModelDiffService diffService;

    public ModelImportController(ModelImportService importer, DomainModelDiffService diffService) {
        this.importer = importer;
        this.diffService = diffService;
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ModelImportResult importModel(@PathVariable UUID projectId,
                                         @RequestBody ModelImportRequest request)
            throws JsonProcessingException {
        return importer.importModel(projectId, request.format(), request.content());
    }

    @PostMapping(path = "/diff", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ModelDiffResult diffModels(@PathVariable UUID projectId,
                                      @RequestBody ModelDiffRequest request) {
        return diffService.diff(projectId, request.before(), request.after());
    }

    public record ModelImportRequest(String format, String content) { }
    public record ModelDiffRequest(JsonNode before, JsonNode after) { }
}
