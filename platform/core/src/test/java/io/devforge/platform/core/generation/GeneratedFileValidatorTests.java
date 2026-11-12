package io.devforge.platform.core.generation;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedFileValidatorTests {

    private final GeneratedFileValidator validator = new GeneratedFileValidator();

    @Test
    void acceptsCanonicalRelativePathsAndKeepsOutputSortedAndImmutable() {
        Map<String, String> validated = validator.validate(Map.of(
                "src/main/java/App.java", "class App {}",
                ".env.example", "DATABASE_PASSWORD=replace-me",
                "README.md", "generated"));

        assertThat(validated.keySet()).containsExactly(".env.example", "README.md", "src/main/java/App.java");
        assertThatThrownBy(() -> validated.put("other.txt", "no"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsTraversalAbsoluteWindowsAndSecretEnvironmentPaths() {
        for (String path : new String[]{"../outside.txt", "src/../../outside.txt", "/etc/passwd",
                "C:/temp/file.txt", "src\\main\\App.java", ".env", "config/.env.production"}) {
            assertThatThrownBy(() -> validator.validate(Map.of(path, "unsafe")))
                    .as("path %s", path)
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Unsafe generated file path");
        }
    }

    @Test
    void rejectsReservedManifestAndNullContent() {
        assertThatThrownBy(() -> validator.validate(Map.of(".devforge/manifest.json", "{}")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot override");
        assertThatThrownBy(() -> validator.validate(java.util.Collections.singletonMap("README.md", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void enforcesFileCountAndPerFileSizeLimits() {
        Map<String, String> tooMany = new HashMap<>();
        for (int i = 0; i <= GeneratedFileValidator.MAX_FILES; i++) {
            tooMany.put("file-" + i + ".txt", "x");
        }
        assertThatThrownBy(() -> validator.validate(tooMany))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("maximum file count");

        assertThatThrownBy(() -> validator.validate(Map.of(
                "large.txt", "x".repeat(GeneratedFileValidator.MAX_FILE_BYTES + 1))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("10 MiB");
    }
}
