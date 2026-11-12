package io.devforge.platform.core.project;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "specification_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uq_specification_snapshots_project_version",
                columnNames = {"project_id", "snapshot_version"})
})
public class SpecificationSnapshotEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;
    @Column(name = "snapshot_version", nullable = false) private int snapshotVersion;
    @Column(name = "schema_version", nullable = false, length = 32) private String schemaVersion;
    @Column(name = "content_sha256", nullable = false, length = 64) private String contentSha256;
    @Column(name = "specification_json", nullable = false, columnDefinition = "text")
    private String specificationJson;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected SpecificationSnapshotEntity() { }

    public SpecificationSnapshotEntity(UUID id, ProjectEntity project, int snapshotVersion,
            String schemaVersion, String contentSha256, String specificationJson) {
        this.id = id;
        this.project = project;
        this.snapshotVersion = snapshotVersion;
        this.schemaVersion = schemaVersion;
        this.contentSha256 = contentSha256;
        this.specificationJson = specificationJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public int getSnapshotVersion() { return snapshotVersion; }
    public String getSchemaVersion() { return schemaVersion; }
    public String getContentSha256() { return contentSha256; }
    public String getSpecificationJson() { return specificationJson; }
    public Instant getCreatedAt() { return createdAt; }
}
