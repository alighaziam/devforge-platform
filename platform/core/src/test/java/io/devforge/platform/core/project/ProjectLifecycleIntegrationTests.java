package io.devforge.platform.core.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectLifecycleIntegrationTests {

    @Autowired private ProjectLifecycleService projects;

    @Test
    void createsProjectAndPersistsImmutableVersionedSpecificationSnapshots() {
        var project = projects.create("Orders", "Order management");
        var first = projects.createSnapshot(project.id(), validSpecification("First"));
        var second = projects.createSnapshot(project.id(), validSpecification("Second"));

        assertThat(first.snapshotVersion()).isEqualTo(1);
        assertThat(second.snapshotVersion()).isEqualTo(2);
        assertThat(first.contentSha256()).hasSize(64).isNotEqualTo(second.contentSha256());
        assertThat(projects.listSnapshots(project.id()))
                .extracting(ProjectLifecycleService.SnapshotView::snapshotVersion)
                .containsExactly(1, 2);
    }

    @Test
    void listsAndReadsCreatedProject() {
        var created = projects.create("Billing", "Billing domain");

        assertThat(projects.get(created.id()).name()).isEqualTo("Billing");
        assertThat(projects.list(0, 20).getContent())
                .extracting(ProjectLifecycleService.ProjectView::id)
                .contains(created.id());
    }

    private String validSpecification(String description) {
        return """
                {
                  "specVersion": "1.0.0",
                  "project": {
                    "id": "orders",
                    "name": "Orders",
                    "description": "%s"
                  },
                  "requirements": {
                    "functional": [],
                    "qualityAttributes": [],
                    "constraints": []
                  },
                  "model": {
                    "entities": [],
                    "relationships": []
                  },
                  "architecture": {
                    "style": "modular-monolith",
                    "decisions": []
                  },
                  "technologySelections": {},
                  "generation": {
                    "profile": "recommended",
                    "outputLayout": "single-project"
                  }
                }
                """.formatted(description);
    }
}
