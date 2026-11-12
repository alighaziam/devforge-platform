package io.devforge.platform.core.project;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 2000) private String description;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(name = "row_version", nullable = false) private long version;

    protected ProjectEntity() { }

    public ProjectEntity(UUID id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
