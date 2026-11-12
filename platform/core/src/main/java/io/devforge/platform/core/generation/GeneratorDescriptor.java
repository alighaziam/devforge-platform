package io.devforge.platform.core.generation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record GeneratorDescriptor(
        String id,
        String version,
        String description,
        Set<String> supportedSpecificationVersions,
        Map<String, String> supportedTechnologyVersions) {

    public GeneratorDescriptor {
        if (id == null || !id.matches("[a-z0-9]+([.-][a-z0-9]+)*")) {
            throw new IllegalArgumentException("Generator id must be a stable lowercase identifier");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Generator version is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Generator description is required");
        }
        supportedSpecificationVersions = Set.copyOf(supportedSpecificationVersions);
        supportedTechnologyVersions = Collections.unmodifiableMap(
                new TreeMap<>(supportedTechnologyVersions));
    }
}
