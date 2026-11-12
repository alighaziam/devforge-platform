package io.devforge.platform.core.project;

import io.devforge.platform.core.specification.SpecificationValidationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/specifications")
public class ProjectSpecificationSnapshotController {

    private final ProjectLifecycleService projects;

    public ProjectSpecificationSnapshotController(ProjectLifecycleService projects) {
        this.projects = projects;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@PathVariable UUID projectId, @RequestBody String json) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(projects.createSnapshot(projectId, json));
        } catch (ProjectLifecycleService.InvalidSpecificationException exception) {
            SpecificationValidationResult validation = exception.getValidation();
            return ResponseEntity.unprocessableEntity().body(validation);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request body is not valid JSON"));
        }
    }

    @GetMapping
    public List<ProjectLifecycleService.SnapshotView> list(@PathVariable UUID projectId) {
        return projects.listSnapshots(projectId);
    }
}
