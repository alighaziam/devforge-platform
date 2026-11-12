package io.devforge.platform.core.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecificationSnapshotRepository extends JpaRepository<SpecificationSnapshotEntity, UUID> {
    Optional<SpecificationSnapshotEntity> findTopByProject_IdOrderBySnapshotVersionDesc(UUID projectId);
    List<SpecificationSnapshotEntity> findAllByProject_IdOrderBySnapshotVersionAsc(UUID projectId);
    Optional<SpecificationSnapshotEntity> findByProject_IdAndSnapshotVersion(UUID projectId, int snapshotVersion);
}
