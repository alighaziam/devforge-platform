package io.devforge.platform.core.architecture;

import java.util.List;

public record ArchitectureDecisionDraft(
        String title,
        String context,
        String decision,
        List<String> alternatives,
        List<String> consequences) {
    public ArchitectureDecisionDraft {
        alternatives = List.copyOf(alternatives);
        consequences = List.copyOf(consequences);
    }
}
