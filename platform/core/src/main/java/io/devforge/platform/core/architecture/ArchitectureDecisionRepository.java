package io.devforge.platform.core.architecture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ArchitectureDecisionRepository extends JpaRepository<ArchitectureDecisionEntity,UUID> {
 List<ArchitectureDecisionEntity> findAllByProject_IdOrderByCreatedAtDesc(UUID projectId);
 Optional<ArchitectureDecisionEntity> findByIdAndProject_Id(UUID id,UUID projectId);
 Optional<ArchitectureDecisionEntity> findBySnapshot_IdAndTitle(UUID snapshotId,String title);
}
