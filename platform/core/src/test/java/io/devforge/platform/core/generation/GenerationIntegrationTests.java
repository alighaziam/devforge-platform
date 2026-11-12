package io.devforge.platform.core.generation;

import io.devforge.platform.core.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GenerationIntegrationTests {

    @Autowired private ProjectLifecycleService projects;
    @Autowired private GenerationService generationService;

    @Test
    void generatesReproducibleProjectArchiveFromValidatedSnapshot() throws Exception {
        var project = projects.create("Orders", "Order management");
        projects.createSnapshot(project.id(), specification(project.id()));

        GenerationArtifact first = generationService.generate(project.id(), 1);
        GenerationArtifact second = generationService.generate(project.id(), 1);
        java.nio.file.Files.write(java.nio.file.Path.of("target/devforge-generated-sample.zip"), first.content());

        assertThat(first.fileName()).isEqualTo("orders.zip");
        assertThat(first.sha256()).isEqualTo(second.sha256());
        assertThat(first.generatedFiles()).isGreaterThanOrEqualTo(8);

        Map<String, String> entries = unzip(first.content());
        assertThat(entries).containsKeys(
                "pom.xml",
                "src/main/java/io/devforge/generated/orders/model/Customer.java",
                "src/main/java/io/devforge/generated/orders/repository/CustomerRepository.java",
                "src/main/resources/db/migration/V1__create_domain_schema.sql",
                ".devforge/manifest.json");
        assertThat(entries.get(".devforge/manifest.json"))
                .contains("devforge.generator.java-spring-postgres")
                .contains(project.id().toString());
    }

    private Map<String, String> unzip(byte[] archive) throws Exception {
        Map<String, String> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                files.put(entry.getName(), new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    private String specification(UUID projectId) {
        return """
                {
                  "specVersion": "1.0.0",
                  "project": { "id": "%s", "name": "Orders", "description": "Order management" },
                  "requirements": { "functional": [], "qualityAttributes": [], "constraints": [] },
                  "model": {
                    "entities": [
                      { "name": "Customer", "attributes": [
                        { "name": "id", "type": "uuid", "required": true },
                        { "name": "email", "type": "string", "required": true, "unique": true }
                      ]}
                    ],
                    "relationships": [],
                    "indexes": []
                  },
                  "architecture": { "style": "layered", "decisions": [] },
                  "technologySelections": {
                    "language": { "id": "language.java", "version": "25" },
                    "backend": { "id": "framework.spring-boot", "version": "4.0.0" },
                    "database": { "id": "database.postgresql", "version": "17" }
                  },
                  "generation": { "profile": "recommended", "outputLayout": "single-project" }
                }
                """.formatted(projectId);
    }
}
