package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratorRegistryTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void resolvesTheOnlyPluginThatSupportsTheSpecification() throws Exception {
        StubPlugin unsupported = new StubPlugin("generator.python", false);
        StubPlugin supported = new StubPlugin("generator.java", true);
        GeneratorRegistry registry = new GeneratorRegistry(List.of(unsupported, supported));

        assertThat(registry.resolve(specification(), null)).isSameAs(supported);
        assertThat(registry.descriptors()).extracting(GeneratorDescriptor::id)
                .containsExactly("generator.java", "generator.python");
    }

    @Test
    void requiresAnExplicitIdWhenMultiplePluginsMatch() throws Exception {
        GeneratorRegistry registry = new GeneratorRegistry(List.of(
                new StubPlugin("generator.python", true),
                new StubPlugin("generator.java", true)));

        assertThatThrownBy(() -> registry.resolve(specification(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void validatesAnExplicitGeneratorSelection() throws Exception {
        StubPlugin supported = new StubPlugin("generator.java", true);
        GeneratorRegistry registry = new GeneratorRegistry(List.of(supported));

        assertThat(registry.resolve(specification(), "generator.java")).isSameAs(supported);
        assertThatThrownBy(() -> registry.resolve(specification(), "generator.unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> registry.resolve(specification(), "generator.python"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void blocksSpecificationsThatHaveNoCompatibleGenerator() throws Exception {
        GeneratorRegistry registry = new GeneratorRegistry(List.of(
                new StubPlugin("generator.java", false)));

        assertThatThrownBy(() -> registry.resolve(specification(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No registered generator supports");
    }

    @Test
    void rejectsDuplicatePluginIdsAtStartup() {
        assertThatThrownBy(() -> new GeneratorRegistry(List.of(
                new StubPlugin("generator.java", true),
                new StubPlugin("generator.java", false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate generator plugin id");
    }

    @Test
    void rejectsAnEmptyRegistryAtStartup() {
        assertThatThrownBy(() -> new GeneratorRegistry(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one trusted generator");
    }

    private JsonNode specification() throws Exception {
        return mapper.readTree("{\"specVersion\":\"1.0.0\"}");
    }

    private static final class StubPlugin implements GeneratorPlugin {
        private final GeneratorDescriptor descriptor;
        private final boolean supported;

        private StubPlugin(String id, boolean supported) {
            this.descriptor = new GeneratorDescriptor(id, "1.0.0", "Test generator",
                    Set.of("1.0.0"), Map.of("language.java", "25"));
            this.supported = supported;
        }

        @Override
        public GeneratorDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean supports(JsonNode specification) {
            return supported && "1.0.0".equals(specification.path("specVersion").asText());
        }

        @Override
        public Map<String, String> generate(JsonNode specification) {
            return Map.of("README.md", "test");
        }
    }
}
