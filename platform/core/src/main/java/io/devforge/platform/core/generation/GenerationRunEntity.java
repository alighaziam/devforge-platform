package io.devforge.platform.core.generation;

import io.devforge.platform.core.project.ProjectEntity;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_runs")
public class GenerationRunEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private SpecificationSnapshotEntity snapshot;
    @Column(name = "generator_id", nullable = false, length = 160) private String generatorId;
    @Column(name = "generator_version", nullable = false, length = 40) private String generatorVersion;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "output_sha256", nullable = false, columnDefinition = "char(64)") private String outputSha256;
    @Column(name = "generated_file_count", nullable = false) private int generatedFileCount;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected GenerationRunEntity() { }

    public GenerationRunEntity(UUID id, ProjectEntity project, SpecificationSnapshotEntity snapshot,
            String generatorId, String generatorVersion, String status,
            String outputSha256, int generatedFileCount) {
        this.id = id;
        this.project = project;
        this.snapshot = snapshot;
        this.generatorId = generatorId;
        this.generatorVersion = generatorVersion;
        this.status = status;
        this.outputSha256 = outputSha256;
        this.generatedFileCount = generatedFileCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public SpecificationSnapshotEntity getSnapshot() { return snapshot; }
    public String getGeneratorId() { return generatorId; }
    public String getGeneratorVersion() { return generatorVersion; }
    public String getStatus() { return status; }
    public String getOutputSha256() { return outputSha256; }
    public int getGeneratedFileCount() { return generatedFileCount; }
    public Instant getCreatedAt() { return createdAt; }
}
