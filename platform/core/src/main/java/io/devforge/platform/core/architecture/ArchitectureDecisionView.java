package io.devforge.platform.core.architecture;
import java.time.Instant;
import java.util.*;
public record ArchitectureDecisionView(UUID id,UUID projectId,UUID snapshotId,int snapshotVersion,String title,String context,String decision,
 List<String> alternatives,List<String> consequences,String status,long version,Instant createdAt,Instant updatedAt,List<ArchitectureDecisionEventView> history) {
 public ArchitectureDecisionView { alternatives=List.copyOf(alternatives);consequences=List.copyOf(consequences);history=List.copyOf(history); }
}
