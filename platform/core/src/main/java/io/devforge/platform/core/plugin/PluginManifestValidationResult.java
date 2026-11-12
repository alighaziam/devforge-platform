package io.devforge.platform.core.plugin;

import java.util.List;

public record PluginManifestValidationResult(boolean valid, List<String> errors) {
    public PluginManifestValidationResult {
        errors = List.copyOf(errors);
    }
}
