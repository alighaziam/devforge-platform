package io.devforge.platform.core.generation;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedFileValidatorTests {

    private final GeneratedFileValidator validator = new GeneratedFileValidator();

    @Test
    void rejectsGeneratorSuppliedReservedManifest() {
        assertThatThrownBy(() -> validator.validate(Map.of(
                ".devforge/manifest.json", "generator must not overwrite this file")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("reserved DevForge manifest");
    }

    @Test
    void acceptsSafeProjectFilesWithoutReservedManifest() {
        assertThatCode(() -> validator.validate(Map.of(
                "pom.xml", "<project/>",
                ".env.example", "DATABASE_PASSWORD=replace-me",
                ".gitignore", ".env",
                "src/main/java/App.java", "class App {}"))).doesNotThrowAnyException();
    }

    @Test
    void rejectsPathTraversalAndAbsolutePaths() {
        assertRejected("../outside.txt");
        assertRejected("src/../../outside.txt");
        assertRejected("/etc/passwd");
        assertRejected("C:/Windows/system.ini");
        assertRejected("src\\\\App.java");
    }

    @Test
    void rejectsSecretsGitMetadataAndReservedManifest() {
        assertRejected(".env");
        assertRejected(".env.production");
        assertRejected(".git/config");
        assertRejected(".devforge/manifest.json");
    }

    @Test
    void rejectsCaseInsensitivePathCollisions() {
        assertThatThrownBy(() -> validator.validate(Map.of(
                "Readme.md", "one",
                "README.md", "two")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("case-insensitive");
    }

    private void assertRejected(String path) {
        assertThatThrownBy(() -> validator.validate(Map.of(path, "content")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
