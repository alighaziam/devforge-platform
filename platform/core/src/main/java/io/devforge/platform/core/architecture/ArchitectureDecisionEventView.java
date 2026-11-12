package io.devforge.platform.core.architecture;
import java.time.Instant;
import java.util.UUID;
public record ArchitectureDecisionEventView(UUID id,String fromStatus,String toStatus,String comment,String actor,Instant createdAt) {
 static ArchitectureDecisionEventView from(ArchitectureDecisionEventEntity e){return new ArchitectureDecisionEventView(e.getId(),e.getFromStatus(),e.getToStatus(),e.getComment(),e.getActor(),e.getCreatedAt());}
}
