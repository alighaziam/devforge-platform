package io.devforge.platform.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainModelDiffServiceTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock ProjectRepository projects;
    @InjectMocks DomainModelDiffService service;

    @Test
    void reportsEntityAttributeRelationshipAndIndexChangesInStablePathOrder() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projects.existsById(projectId)).thenReturn(true);
        var before = mapper.readTree("""
                {
                  "entities": [
                    {"name":"Customer","attributes":[
                      {"name":"id","type":"uuid","required":true},
                      {"name":"email","type":"string","required":true}
                    ]},
                    {"name":"Legacy","attributes":[]}
                  ],
                  "relationships":[{"name":"placedBy","from":"Order","to":"Customer","cardinality":"many-to-one"}],
                  "indexes":[{"name":"idx_customer_email","entity":"Customer","columns":["email"]}]
                }
                """);
        var after = mapper.readTree("""
                {
                  "entities": [
                    {"name":"Customer","attributes":[
                      {"name":"id","type":"uuid","required":true},
                      {"name":"email","type":"string","required":true,"unique":true},
                      {"name":"displayName","type":"string"}
                    ]},
                    {"name":"Order","attributes":[]}
                  ],
                  "relationships":[{"name":"placedBy","from":"Order","to":"Customer","cardinality":"one-to-one"}],
                  "indexes":[{"name":"idx_customer_display_name","entity":"Customer","columns":["displayName"]}]
                }
                """);

        var result = service.diff(projectId, before, after);

        assertThat(result.changes()).extracting(ModelDiffResult.ModelChange::path).containsExactly(
                "/entities/Customer/attributes/displayName",
                "/entities/Customer/attributes/email",
                "/entities/Legacy",
                "/entities/Order",
                "/indexes/idx_customer_display_name",
                "/indexes/idx_customer_email",
                "/relationships/placedBy");
        assertThat(result.changes()).extracting(ModelDiffResult.ModelChange::changeType)
                .containsExactly("added", "modified", "removed", "added", "added", "removed", "modified");
    }

    @Test
    void rejectsDuplicateEntityNamesInsteadOfProducingAmbiguousDiffs() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projects.existsById(projectId)).thenReturn(true);
        var duplicate = mapper.readTree("""
                {"entities":[{"name":"Customer"},{"name":"Customer"}]}
                """);

        assertThatThrownBy(() -> service.diff(projectId, duplicate, duplicate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("duplicate identity");
    }

    @Test
    void rejectsInvalidModelsAndOversizedSnapshots() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projects.existsById(projectId)).thenReturn(true);
        var invalid = mapper.readTree("{\"entities\":{}}");
        assertThatThrownBy(() -> service.diff(projectId, invalid, invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("entities array");

        String largeText = "x".repeat(270_000);
        var large = mapper.readTree("{\"entities\":[],\"metadata\":\"" + largeText + "\"}");
        assertThatThrownBy(() -> service.diff(projectId, large, large))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("512 KiB");
    }
}
