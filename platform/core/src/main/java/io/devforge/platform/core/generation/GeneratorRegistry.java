package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class GeneratorRegistry {

    private final Map<String, GeneratorPlugin> plugins;

    public GeneratorRegistry(List<GeneratorPlugin> discoveredPlugins) {
        Map<String, GeneratorPlugin> registered = new TreeMap<>();
        for (GeneratorPlugin plugin : discoveredPlugins) {
            GeneratorDescriptor descriptor = plugin.descriptor();
            GeneratorPlugin previous = registered.putIfAbsent(descriptor.id(), plugin);
            if (previous != null) {
                throw new IllegalStateException("Duplicate generator plugin id: " + descriptor.id());
            }
        }
        if (registered.isEmpty()) {
            throw new IllegalStateException("At least one trusted generator plugin must be registered");
        }
        this.plugins = java.util.Collections.unmodifiableMap(registered);
    }

    public List<GeneratorDescriptor> descriptors() {
        return plugins.values().stream()
                .map(GeneratorPlugin::descriptor)
                .sorted(java.util.Comparator.comparing(GeneratorDescriptor::id))
                .toList();
    }

    public GeneratorPlugin resolve(JsonNode specification, String requestedGeneratorId) {
        if (requestedGeneratorId != null && !requestedGeneratorId.isBlank()) {
            GeneratorPlugin requested = plugins.get(requestedGeneratorId);
            if (requested == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown generator plugin: " + requestedGeneratorId);
            }
            if (!requested.supports(specification)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Generator " + requestedGeneratorId
                                + " does not support the selected technologies or project constraints");
            }
            return requested;
        }

        List<GeneratorPlugin> matches = plugins.values().stream()
                .filter(plugin -> plugin.supports(specification))
                .toList();
        if (matches.isEmpty()) {
            String available = String.join(", ", plugins.keySet());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No registered generator supports the selected project specification. Available generators: "
                            + available);
        }
        if (matches.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Multiple generators support this project. Specify generatorId explicitly.");
        }
        return matches.getFirst();
    }
}
