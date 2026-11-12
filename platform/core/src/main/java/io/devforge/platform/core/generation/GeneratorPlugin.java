package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Trusted, in-process generator extension point.
 *
 * External plugins must not be loaded as Java classes; they require an isolated
 * runtime adapter and sandbox policy before they can be registered.
 */
public interface GeneratorPlugin {
    GeneratorDescriptor descriptor();

    boolean supports(JsonNode specification);

    Map<String, String> generate(JsonNode specification);
}
