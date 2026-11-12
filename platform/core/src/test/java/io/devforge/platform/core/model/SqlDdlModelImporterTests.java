package io.devforge.platform.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlDdlModelImporterTests {

    private final SqlDdlModelImporter importer = new SqlDdlModelImporter();

    @Test
    void importsSimplePostgresTablesAndIndexes() {
        Map<String, Object> model = importer.importDdl("""
                CREATE TABLE customer (
                    id UUID PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    age INTEGER,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL
                );
                CREATE INDEX idx_customer_age ON customer (age);
                """);

        List<?> entities = (List<?>) model.get("entities");
        assertThat(entities).hasSize(1);
        assertThat((List<?>) model.get("relationships")).isEmpty();
        assertThat((List<?>) model.get("indexes")).hasSize(1);
        assertThat(entities.get(0).toString()).contains("customer", "email", "datetime");
    }

    @Test
    void rejectsForeignKeysAndCompositePrimaryKeysExplicitly() {
        assertThatThrownBy(() -> importer.importDdl("""
                CREATE TABLE orders (
                    id UUID PRIMARY KEY,
                    customer_id UUID REFERENCES customer(id)
                );
                """)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);

        assertThatThrownBy(() -> importer.importDdl("""
                CREATE TABLE join_table (
                    left_id UUID,
                    right_id UUID,
                    PRIMARY KEY (left_id, right_id)
                );
                """)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void rejectsAnIdColumnThatIsNotDeclaredAsThePrimaryKey() {
        assertThatThrownBy(() -> importer.importDdl("CREATE TABLE customer (id UUID, email VARCHAR(255));"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void rejectsIndexesThatReferenceUnknownColumns() {
        assertThatThrownBy(() -> importer.importDdl("""
                CREATE TABLE customer (id UUID PRIMARY KEY);
                CREATE INDEX idx_customer_email ON customer (email);
                """)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
