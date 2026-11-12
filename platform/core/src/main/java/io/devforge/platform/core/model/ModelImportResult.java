package io.devforge.platform.core.model;

import java.util.List;
import java.util.Map;

public record ModelImportResult(Map<String, Object> model, List<String> warnings) {
    public ModelImportResult {
        model = Map.copyOf(model);
        warnings = List.copyOf(warnings);
    }
}
