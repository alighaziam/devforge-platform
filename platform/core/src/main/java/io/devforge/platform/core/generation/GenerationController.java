package io.devforge.platform.core.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/generations")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public ResponseEntity<byte[]> generate(
            @PathVariable UUID projectId,
            @RequestBody GenerationRequest request) throws JsonProcessingException {
        GenerationArtifact artifact = generationService.generate(projectId, request.snapshotVersion());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(artifact.fileName()).build());
        headers.set("X-Generation-SHA256", artifact.sha256());
        headers.set("X-Generation-Run-Id", artifact.runId().toString());
        headers.set("X-Generated-Files", Integer.toString(artifact.generatedFiles()));
        return new ResponseEntity<>(artifact.content(), headers, HttpStatus.OK);
    }

    public record GenerationRequest(int snapshotVersion) { }
}
