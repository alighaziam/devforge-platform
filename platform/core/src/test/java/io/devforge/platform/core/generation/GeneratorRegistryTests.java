package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratorRegistryTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registersMultipleVersionsAndResolvesExactCoordinates() throws Exception {
        GeneratorPlugin v1 = plugin("devforge.generator.sample", "1.0.0", true);
        GeneratorPlugin v2 = plugin("devforge.generator.sample", "2.0.0", true);
        GeneratorRegistry registry = new GeneratorRegistry(java.util.List.of(v2, v1));

        assertThat(registry.descriptors()).extracting(GeneratorDescriptor::version)
                .containsExactly("1.0.0", "2.0.0");
        assertThat(registry.resolve(specification(), "devforge.generator.sample@1.0.0")).isSameAs(v1);
        assertThat(registry.resolve(specification(), "devforge.generator.sample@2.0.0")).isSameAs(v2);
    }

    @Test
    void requiresAnExactVersionWhenSeveralVersionsMatch() throws Exception {
        GeneratorRegistry registry = new GeneratorRegistry(java.util.List.of(
                plugin("devforge.generator.sample", "1.0.0", true),
                plugin("devforge.generator.sample", "2.0.0", true)));

        assertThatThrownBy(() -> registry.resolve(specification(), "devforge.generator.sample"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("id@version")
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectsUnknownCoordinatesAndUnsupportedSpecifications() throws Exception {
        GeneratorRegistry registry = new GeneratorRegistry(java.util.List.of(
                plugin("devforge.generator.sample", "1.0.0", false)));

        assertThatThrownBy(() -> registry.resolve(specification(), "devforge.generator.missing@1.0.0"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unknown generator plugin coordinate");
        assertThatThrownBy(() -> registry.resolve(specification(), "devforge.generator.sample@1.0.0"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not support");
    }

    @Test
    void rejectsDuplicateCoordinatesAndInvalidVersions() {
        GeneratorPlugin first = plugin("devforge.generator.sample", "1.0.0", true);
        GeneratorPlugin duplicate = plugin("devforge.generator.sample", "1.0.0", true);
        assertThatThrownBy(() -> new GeneratorRegistry(java.util.List.of(first, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate generator plugin coordinate");
        assertThatThrownBy(() -> new GeneratorDescriptor("devforge.generator.sample", "latest",
                "sample", Set.of("1.0.0"), Map.of("language.java", "25")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semantic versioning");
    }

    private JsonNode specification() throws Exception {
        return mapper.readTree("{\"specVersion\":\"1.0.0\"}");
    }

    private GeneratorPlugin plugin(String id, String version, boolean supported) {
        return new GeneratorPlugin() {
            private final GeneratorDescriptor descriptor = new GeneratorDescriptor(id, version,
                    "Test generator", Set.of("1.0.0"), Map.of("language.java", "25"));

            @Override public GeneratorDescriptor descriptor() { return descriptor; }
            @Override public boolean supports(JsonNode specification) { return supported; }
            @Override public Map<String, String> generate(JsonNode specification) { return Map.of("README.md", "test"); }
        };
    }
}
