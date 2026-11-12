package io.devforge.platform.core.catalog;

import java.util.List;

public record CompatibilityFinding(
        String ruleId,
        String status,
        String severity,
        String message,
        String rationale,
        List<String> affectedTechnologyIds,
        List<String> remediation,
        List<String> evidence,
        String overridePolicy) {

    public CompatibilityFinding {
        affectedTechnologyIds = List.copyOf(affectedTechnologyIds);
        remediation = List.copyOf(remediation);
        evidence = List.copyOf(evidence);
    }
}
