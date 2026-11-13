package io.devforge.platform.core.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectLifecycleIntegrationTests {

    @Autowired private ProjectLifecycleService projects;

    @Test
    void createsProjectAndPersistsImmutableVersionedSpecificationSnapshots() throws Exception {
        var project = projects.create("Orders", "Order management");
        var first = projects.createSnapshot(project.id(), validSpecification(project.id(), "First"));
        var second = projects.createSnapshot(project.id(), validSpecification(project.id(), "Second"));

        assertThat(first.snapshotVersion()).isEqualTo(1);
        assertThat(second.snapshotVersion()).isEqualTo(2);
        assertThat(first.contentSha256()).hasSize(64).isNotEqualTo(second.contentSha256());
        assertThat(projects.listSnapshots(project.id()))
                .extracting(ProjectLifecycleService.SnapshotView::snapshotVersion)
                .containsExactly(1, 2);

        var stored = projects.readSnapshot(project.id(), 1);
        assertThat(stored.schemaVersion()).isEqualTo("1.0.0");
        assertThat(stored.contentSha256()).isEqualTo(first.contentSha256());
        assertThat(stored.specification().path("project").path("description").asText()).isEqualTo("First");
        assertThat(stored.specification().path("project").path("id").asText()).isEqualTo(project.id().toString());
    }

    @Test
    void rejectsMissingAndInvalidSnapshotVersions() {
        var project = projects.create("Orders", "Order management");

        assertThatThrownBy(() -> projects.readSnapshot(project.id(), 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> projects.readSnapshot(project.id(), 4))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listsAndReadsCreatedProject() {
        var created = projects.create("Billing", "Billing domain");

        assertThat(projects.get(created.id()).name()).isEqualTo("Billing");
        assertThat(projects.list(0, 20).getContent())
                .extracting(ProjectLifecycleService.ProjectView::id)
                .contains(created.id());
    }

    @Test
    void filtersPagesUpdatesAndDeletesProjects() throws Exception {
        var billing = projects.create("Billing", "Invoice ledger");
        projects.create("Shipping", "Warehouse routes");
        projects.createSnapshot(billing.id(), validSpecification(billing.id(), "Invoice ledger"));

        assertThat(projects.list(0, 1, null, "ACTIVE").getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(projects.list(0, 20, "invoice", null).getContent())
                .extracting(ProjectLifecycleService.ProjectView::id)
                .containsExactly(billing.id());
        assertThat(projects.list(0, 20, null, "ARCHIVED").getTotalElements()).isZero();

        var updated = projects.update(billing.id(), "Billing platform", "Customer invoices", billing.version());
        assertThat(updated.name()).isEqualTo("Billing platform");
        assertThat(updated.version()).isEqualTo(billing.version() + 1);

        projects.delete(billing.id());
        assertThat(projects.list(0, 20, "invoice", null).getTotalElements()).isZero();
        assertThatThrownBy(() -> projects.get(billing.id()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsStaleProjectUpdatesAndInvalidFilters() {
        var billing = projects.create("Billing", "Invoice ledger");

        assertThatThrownBy(() -> projects.update(billing.id(), "Billing", "Changed", billing.version() + 5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> projects.list(0, 20, null, "active"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private String validSpecification(java.util.UUID projectId, String description) {
        return """
                {
                  "specVersion": "1.0.0",
                  "project": {
                    "id": "%s",
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
                """.formatted(projectId, description);
    }
}
