package io.devforge.platform.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ModelDiffResult(List<ModelChange> changes) {
    public ModelDiffResult {
        changes = List.copyOf(changes);
    }

    public record ModelChange(String path, String changeType, JsonNode before, JsonNode after) { }
}
