package io.devforge.platform.core.plugin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManifestValidatorTests {

    private final PluginManifestValidator validator = new PluginManifestValidator();

    @Test
    void acceptsAValidLeastPrivilegePluginManifest() throws Exception {
        String manifest = """
                {
                  "manifestVersion": "1.0.0",
                  "plugin": {
                    "id": "sample.java-generator",
                    "name": "Sample Java Generator",
                    "version": "0.1.0",
                    "publisher": "DevForge",
                    "kind": "generator"
                  },
                  "capabilities": ["generator.backend"],
                  "compatibility": {
                    "platformApi": "1.0.0",
                    "specificationVersions": ["1.0.0"],
                    "technologies": ["language.java"],
                    "operatingSystems": ["linux"]
                  },
                  "permissions": ["workspace.read", "artifact.write"],
                  "entrypoints": {
                    "protocol": "oci",
                    "command": ["/opt/devforge/plugin"]
                  }
                }
                """;

        assertThat(validator.validate(manifest).valid()).isTrue();
    }

    @Test
    void rejectsUnknownPermissionsAndMissingRequiredMetadata() throws Exception {
        String manifest = """
                {
                  "manifestVersion": "1.0.0",
                  "plugin": { "id": "bad", "name": "Bad" },
                  "capabilities": ["generator.backend"],
                  "compatibility": {},
                  "permissions": ["root.access"],
                  "entrypoints": {}
                }
                """;

        PluginManifestValidationResult result = validator.validate(manifest);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
}
