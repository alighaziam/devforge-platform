package io.devforge.platform.core.architecture;

import java.util.List;

public record ArchitectureFinding(
        String id,
        String status,
        String severity,
        String message,
        String rationale,
        List<String> affectedPaths,
        List<String> remediation) {
    public ArchitectureFinding {
        affectedPaths = List.copyOf(affectedPaths);
        remediation = List.copyOf(remediation);
    }
}
