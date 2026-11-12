package io.devforge.platform.core.specification;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/specifications")
public class ProjectSpecificationController {

    private final ProjectSpecificationValidator validator;

    public ProjectSpecificationController(ProjectSpecificationValidator validator) {
        this.validator = validator;
    }

    @PostMapping(path = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validate(@RequestBody String payload) {
        try {
            SpecificationValidationResult result = validator.validate(payload);
            return ResponseEntity.status(result.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(result);
        } catch (JsonProcessingException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request body is not valid JSON"));
        }
    }
}
