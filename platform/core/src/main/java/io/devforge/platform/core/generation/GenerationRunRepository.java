package io.devforge.platform.core.generation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, UUID> {
    List<GenerationRunEntity> findAllByProject_IdOrderByCreatedAtAsc(UUID projectId);
    List<GenerationRunEntity> findAllByProject_IdOrderByCreatedAtDesc(UUID projectId);
}
