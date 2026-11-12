package io.devforge.platform.core.architecture;

import java.util.List;

public record ArchitectureAssessmentResult(
        String selectedStyle,
        String recommendedStyle,
        String rationale,
        boolean requiresReview,
        List<ArchitectureFinding> findings,
        List<String> tradeOffs,
        ArchitectureDecisionDraft adrDraft) {
    public ArchitectureAssessmentResult {
        findings = List.copyOf(findings);
        tradeOffs = List.copyOf(tradeOffs);
    }
}
