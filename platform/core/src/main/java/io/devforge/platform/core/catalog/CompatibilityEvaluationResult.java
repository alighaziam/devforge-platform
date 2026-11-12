package io.devforge.platform.core.catalog;

import java.util.List;

public record CompatibilityEvaluationResult(
        boolean compatible,
        boolean requiresReview,
        List<CompatibilityFinding> findings) {

    public CompatibilityEvaluationResult {
        findings = List.copyOf(findings);
    }
}
