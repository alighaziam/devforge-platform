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

    @Test
    void readsRequirementsAndSourceArtifactsFromStoredSnapshot() throws Exception {
        var project = projects.create("Billing", "Invoice ledger");
        String digest = "a".repeat(64);
        projects.createSnapshot(project.id(), """
                {
                  "specVersion": "1.0.0",
                  "project": { "id": "%s", "name": "Billing", "description": "Invoice ledger" },
                  "requirements": {
                    "functional": [{
                      "id": "FR-1",
                      "statement": "Record an invoice",
                      "priority": "must",
                      "acceptanceCriteria": ["The invoice total is stored"],
                      "sourceRefs": ["brief.pdf"]
                    }],
                    "qualityAttributes": [{ "attribute": "security", "target": "authenticated", "priority": "high" }],
                    "constraints": ["Amounts use decimal currency"],
                    "sourceArtifacts": [{
                      "id": "brief",
                      "mediaType": "application/pdf",
                      "sha256": "%s",
                      "sourceName": "brief.pdf"
                    }]
                  },
                  "model": { "entities": [], "relationships": [] },
                  "architecture": { "style": "modular-monolith", "decisions": [] },
                  "technologySelections": {},
                  "generation": { "profile": "recommended", "outputLayout": "single-project" }
                }
                """.formatted(project.id(), digest));

        var requirements = projects.readRequirements(project.id(), 1);
        assertThat(requirements.snapshotVersion()).isEqualTo(1);
        assertThat(requirements.functional()).hasSize(1);
        assertThat(requirements.functional().get(0).path("id").asText()).isEqualTo("FR-1");
        assertThat(requirements.qualityAttributes().get(0).path("attribute").asText()).isEqualTo("security");
        assertThat(requirements.constraints()).extracting(node -> node.asText())
                .containsExactly("Amounts use decimal currency");
        assertThat(requirements.sourceArtifacts().get(0).path("sha256").asText()).isEqualTo(digest);
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

    @Test
    void projectOwnershipIsScopedToAuthenticatedSubject() {
        try {
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "tenant-alice", "n/a", java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_devforge.write")));
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        var created = projects.create("Alice private project", "Tenant isolation");

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "tenant-bob", "n/a", java.util.List.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_devforge.read"))));
        org.springframework.web.server.ResponseStatusException hidden = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> projects.get(created.id()));
        org.junit.jupiter.api.Assertions.assertEquals(404, hidden.getStatusCode().value());

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        org.junit.jupiter.api.Assertions.assertEquals(created.id(), projects.get(created.id()).id());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }




}
