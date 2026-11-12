package io.devforge.platform.core.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CompatibilityEvaluationRequest(Map<String, String> selections) {

    public CompatibilityEvaluationRequest {
        selections = selections == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(selections));
    }
}
