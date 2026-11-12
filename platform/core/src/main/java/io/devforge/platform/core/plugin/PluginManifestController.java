package io.devforge.platform.core.plugin;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plugins/manifests")
public class PluginManifestController {

    private static final int MAX_PAYLOAD_BYTES = 1_048_576;
    private final PluginManifestValidator validator;

    public PluginManifestController(PluginManifestValidator validator) {
        this.validator = validator;
    }

    @PostMapping(path = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validate(@RequestBody String payload) {
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "Manifest payload exceeds the 1 MiB limit"));
        }
        try {
            PluginManifestValidationResult result = validator.validate(payload);
            return ResponseEntity.status(result.valid()
                    ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request body is not valid JSON"));
        }
    }
}
