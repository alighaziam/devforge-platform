package io.devforge.platform.core.architecture;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="architecture_decision_events")
public class ArchitectureDecisionEventEntity {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="decision_id",nullable=false) private ArchitectureDecisionEntity decision;
 @Column(name="from_status",length=24) private String fromStatus;
 @Column(name="to_status",nullable=false,length=24) private String toStatus;
 @Column(name="comment_text",nullable=false,length=2000) private String comment;
 @Column(nullable=false,length=160) private String actor;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected ArchitectureDecisionEventEntity(){}
 public ArchitectureDecisionEventEntity(UUID id,ArchitectureDecisionEntity decision,String fromStatus,String toStatus,String comment,String actor){
  this.id=id;this.decision=decision;this.fromStatus=fromStatus;this.toStatus=toStatus;this.comment=comment;this.actor=actor;this.createdAt=Instant.now();
 }
 public UUID getId(){return id;} public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;}
 public String getComment(){return comment;} public String getActor(){return actor;} public Instant getCreatedAt(){return createdAt;}
}
