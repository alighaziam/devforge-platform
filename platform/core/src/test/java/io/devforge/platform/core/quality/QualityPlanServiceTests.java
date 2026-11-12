package io.devforge.platform.core.quality;

import io.devforge.platform.core.project.ProjectEntity;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import io.devforge.platform.core.project.SpecificationSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualityPlanServiceTests {

    @Test
    void createsIndependentFunctionalDataAndSecurityTestCasesFromSnapshot() throws Exception {
        UUID projectId = UUID.randomUUID();
        var project = new ProjectEntity(projectId, "Orders", "Order service");
        var snapshot = new SpecificationSnapshotEntity(UUID.randomUUID(), project, 1,
                "1.0.0", "a".repeat(64), specification(projectId));
        SpecificationSnapshotRepository repository = mock(SpecificationSnapshotRepository.class);
        when(repository.findByProject_IdAndSnapshotVersion(projectId, 1))
                .thenReturn(Optional.of(snapshot));

        QualityPlanResult plan = new QualityPlanService(repository).createPlan(projectId, 1);

        assertThat(plan.testCases()).extracting(QualityTestCase::id)
                .contains("TEST-FR-FR-1-AC-1", "TEST-DATA-CUSTOMER-CREATE",
                        "TEST-DATA-CUSTOMER-EMAIL-REQUIRED",
                        "TEST-DATA-CUSTOMER-EMAIL-UNIQUE", "TEST-SEC-AUTHN", "TEST-SEC-MFA");
        assertThat(plan.summary()).containsKeys("FUNCTIONAL", "DATA_MODEL", "DATA_VALIDATION", "DATA_INTEGRITY", "SECURITY");
    }

    private String specification(UUID projectId) {
        return """
                {
                  "specVersion": "1.0.0",
                  "project": { "id": "%s", "name": "Orders", "description": "Order service" },
                  "requirements": {
                    "functional": [
                      { "id": "FR-1", "statement": "Create an order", "priority": "must",
                        "acceptanceCriteria": ["A valid order is persisted"] }
                    ],
                    "qualityAttributes": [
                      { "attribute": "availability", "priority": "high", "target": "99.9%%", "measure": "monthly uptime" }
                    ],
                    "constraints": ["All order writes must be audited"]
                  },
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
                  "architecture": { "style": "modular-monolith", "decisions": [] },
                  "technologySelections": {},
                  "security": { "authentication": "oidc", "mfaRequired": true },
                  "generation": { "profile": "recommended", "outputLayout": "single-project" }
                }
                """.formatted(projectId);
    }
}
