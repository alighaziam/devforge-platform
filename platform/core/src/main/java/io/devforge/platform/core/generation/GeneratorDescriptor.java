package io.devforge.platform.core.generation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
        if (version == null || !version.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?")) {
            throw new IllegalArgumentException("Generator version must use semantic versioning (major.minor.patch)");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Generator description is required");
        }
        if (supportedSpecificationVersions == null || supportedSpecificationVersions.isEmpty()) {
            throw new IllegalArgumentException("At least one supported specification version is required");
        }
        if (supportedTechnologyVersions == null || supportedTechnologyVersions.isEmpty()) {
            throw new IllegalArgumentException("Supported technology versions are required");
        }
        supportedSpecificationVersions = Collections.unmodifiableSet(
                new TreeSet<>(supportedSpecificationVersions));
        supportedTechnologyVersions = Collections.unmodifiableMap(
                new TreeMap<>(supportedTechnologyVersions));
    }

    public String coordinate() {
        return id + "@" + version;
    }
}
