package io.devforge.platform.core.architecture;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/architecture")
public class ArchitectureAssessmentController {
    private final ArchitectureAssessmentService assessments;

    public ArchitectureAssessmentController(ArchitectureAssessmentService assessments) {
        this.assessments = assessments;
    }

    @PostMapping("/evaluate")
    public ArchitectureAssessmentResult evaluate(@PathVariable UUID projectId,
            @RequestBody AssessmentRequest request) throws JsonProcessingException {
        return assessments.evaluate(projectId, request.snapshotVersion());
    }

    public record AssessmentRequest(int snapshotVersion) { }
}
