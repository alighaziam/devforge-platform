package io.devforge.platform.core.project;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from ProjectEntity project where project.id = :id")
    Optional<ProjectEntity> findByIdForUpdate(@Param("id") UUID id);
}
