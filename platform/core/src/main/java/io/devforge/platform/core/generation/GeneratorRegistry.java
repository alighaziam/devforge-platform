package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class GeneratorRegistry {

    private final Map<String, GeneratorPlugin> pluginsByCoordinate;
    private final Map<String, List<GeneratorPlugin>> pluginsById;

    public GeneratorRegistry(List<GeneratorPlugin> discoveredPlugins) {
        Map<String, GeneratorPlugin> byCoordinate = new TreeMap<>();
        Map<String, List<GeneratorPlugin>> byId = new TreeMap<>();
        for (GeneratorPlugin plugin : discoveredPlugins) {
            GeneratorDescriptor descriptor = Objects.requireNonNull(plugin.descriptor(),
                    "Generator plugin descriptor is required");
            GeneratorPlugin previous = byCoordinate.putIfAbsent(descriptor.coordinate(), plugin);
            if (previous != null) {
                throw new IllegalStateException("Duplicate generator plugin coordinate: " + descriptor.coordinate());
            }
            byId.computeIfAbsent(descriptor.id(), ignored -> new ArrayList<>()).add(plugin);
        }
        if (byCoordinate.isEmpty()) {
            throw new IllegalStateException("At least one trusted generator plugin must be registered");
        }
        byId.replaceAll((id, versions) -> versions.stream()
                .sorted(Comparator.comparing(plugin -> plugin.descriptor().version()))
                .toList());
        this.pluginsByCoordinate = Collections.unmodifiableMap(byCoordinate);
        this.pluginsById = Collections.unmodifiableMap(byId);
    }

    public List<GeneratorDescriptor> descriptors() {
        return pluginsByCoordinate.values().stream()
                .map(GeneratorPlugin::descriptor)
                .sorted(Comparator.comparing(GeneratorDescriptor::id)
                        .thenComparing(GeneratorDescriptor::version))
                .toList();
    }

    public GeneratorPlugin resolve(JsonNode specification, String requestedGenerator) {
        if (requestedGenerator != null && !requestedGenerator.isBlank()) {
            if (requestedGenerator.contains("@")) {
                GeneratorPlugin exact = pluginsByCoordinate.get(requestedGenerator);
                if (exact == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Unknown generator plugin coordinate: " + requestedGenerator);
                }
                return requireSupport(exact, specification);
            }

            List<GeneratorPlugin> registered = pluginsById.get(requestedGenerator);
            if (registered == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown generator plugin: " + requestedGenerator);
            }
            List<GeneratorPlugin> matches = registered.stream()
                    .filter(plugin -> plugin.supports(specification))
                    .toList();
            if (matches.isEmpty()) {
                throw unsupported(requestedGenerator);
            }
            if (matches.size() > 1) {
                throw ambiguous(requestedGenerator);
            }
            return matches.getFirst();
        }

        List<GeneratorPlugin> matches = pluginsByCoordinate.values().stream()
                .filter(plugin -> plugin.supports(specification))
                .toList();
        if (matches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No registered generator supports the selected project specification. Available generators: "
                            + String.join(", ", pluginsByCoordinate.keySet()));
        }
        if (matches.size() > 1) {
            throw ambiguous("generatorId");
        }
        return matches.getFirst();
    }

    private GeneratorPlugin requireSupport(GeneratorPlugin plugin, JsonNode specification) {
        if (!plugin.supports(specification)) {
            throw unsupported(plugin.descriptor().coordinate());
        }
        return plugin;
    }

    private ResponseStatusException unsupported(String requested) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Generator " + requested + " does not support the selected specification");
    }

    private ResponseStatusException ambiguous(String requested) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Multiple generator versions or plugins support this project. Specify generatorId as id@version (requested: "
                        + requested + ").");
    }
}
