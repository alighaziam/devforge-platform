package io.devforge.platform.core.architecture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ArchitectureDecisionEventRepository extends JpaRepository<ArchitectureDecisionEventEntity,UUID> {
 List<ArchitectureDecisionEventEntity> findAllByDecision_IdOrderByCreatedAtAsc(UUID decisionId);
}
