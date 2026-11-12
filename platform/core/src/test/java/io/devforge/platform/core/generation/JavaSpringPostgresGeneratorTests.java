package io.devforge.platform.core.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSpringPostgresGeneratorTests {

    private final JavaSpringPostgresGenerator generator = new JavaSpringPostgresGenerator();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void generatesDeterministicSpringBootProjectFromDomainModel() throws Exception {
        var spec = mapper.readTree(specification());
        Map<String, String> files = generator.generate(spec);

        assertThat(files).containsKeys(
                "pom.xml",
                "src/main/java/io/devforge/generated/orders/model/Customer.java",
                "src/main/java/io/devforge/generated/orders/repository/CustomerRepository.java",
                "src/main/java/db/migration/V1__CreateDomainSchema.java",
                "Dockerfile",
                "docker-compose.yml");
        assertThat(files.get("pom.xml")).contains("spring-boot-starter-actuator");
        assertThat(files.get("docker-compose.yml")).contains("pg_isready", "service_healthy");
        assertThat(files.get("src/main/resources/application.yml")).contains("management:", "include: health");
        assertThat(files.get("src/main/java/db/migration/V1__CreateDomainSchema.java"))
                .contains("extends BaseJavaMigration")
                .contains("CREATE TABLE customer")
                .contains("email VARCHAR(255) NOT NULL UNIQUE");
        assertThat(files.get("src/main/java/io/devforge/generated/orders/model/Customer.java"))
                .contains("@Entity")
                .contains("private String email;");
    }

    @Test
    void rejectsUnsafeOrUnsupportedModelAttributes() throws Exception {
        String invalid = specification().replace("\"type\": \"string\"", "\"type\": \"object\"");
        var spec = mapper.readTree(invalid);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> generator.generate(spec))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }


    @Test
    void rejectsIndexesWithUnknownTablesOrColumns() throws Exception {
        ObjectNode spec = (ObjectNode) mapper.readTree(specification());
        ObjectNode index = mapper.createObjectNode().put("entity", "Customer").put("name", "idx_customer_missing");
        index.putArray("columns").add("missing");
        spec.withObject("/model").putArray("indexes").add(index);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> generator.generate(spec))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("unknown column");
    }

    @Test
    void rejectsUnsupportedPostgresIndexMethods() throws Exception {
        ObjectNode spec = (ObjectNode) mapper.readTree(specification());
        ObjectNode index = mapper.createObjectNode()
                .put("entity", "Customer")
                .put("name", "idx_customer_email")
                .put("method", "gin");
        index.putArray("columns").add("email");
        spec.withObject("/model").putArray("indexes").add(index);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> generator.generate(spec))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("btree indexes only");
    }

    private String specification() {
        return """
                {
                  "specVersion": "1.0.0",
                  "project": { "id": "orders", "name": "Orders", "description": "Order service" },
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
                """;
    }
}
