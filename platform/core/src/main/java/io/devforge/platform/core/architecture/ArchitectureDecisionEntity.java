package io.devforge.platform.core.architecture;
import io.devforge.platform.core.project.ProjectEntity;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="architecture_decisions")
public class ArchitectureDecisionEntity {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id",nullable=false) private ProjectEntity project;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="snapshot_id",nullable=false) private SpecificationSnapshotEntity snapshot;
 @Column(nullable=false,length=240) private String title;
 @Column(name="context_text",nullable=false,columnDefinition="text") private String context;
 @Column(name="decision_text",nullable=false,columnDefinition="text") private String decision;
 @Column(name="alternatives_json",nullable=false,columnDefinition="text") private String alternativesJson;
 @Column(name="consequences_json",nullable=false,columnDefinition="text") private String consequencesJson;
 @Column(nullable=false,length=24) private String status;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Version @Column(name="row_version",nullable=false) private long version;
 protected ArchitectureDecisionEntity() {}
 public ArchitectureDecisionEntity(UUID id,ProjectEntity project,SpecificationSnapshotEntity snapshot,String title,String context,String decision,String alternativesJson,String consequencesJson) {
  this.id=id;this.project=project;this.snapshot=snapshot;this.title=title;this.context=context;this.decision=decision;
  this.alternativesJson=alternativesJson;this.consequencesJson=consequencesJson;this.status="DRAFT";this.createdAt=Instant.now();this.updatedAt=createdAt;
 }
 public void transitionTo(String target){status=target;updatedAt=Instant.now();}
 public UUID getId(){return id;} public ProjectEntity getProject(){return project;} public SpecificationSnapshotEntity getSnapshot(){return snapshot;}
 public String getTitle(){return title;} public String getContext(){return context;} public String getDecision(){return decision;}
 public String getAlternativesJson(){return alternativesJson;} public String getConsequencesJson(){return consequencesJson;}
 public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
