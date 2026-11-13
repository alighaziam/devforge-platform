package io.devforge.platform.core.project;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    Optional<ProjectEntity> findByIdAndOwnerSubject(UUID id, String ownerSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from ProjectEntity project where project.id = :id")
    Optional<ProjectEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from ProjectEntity project where project.id = :id and project.ownerSubject = :owner")
    Optional<ProjectEntity> findByIdAndOwnerSubjectForUpdate(
            @Param("id") UUID id, @Param("owner") String owner);

    @Query("""
            select project from ProjectEntity project
            where project.ownerSubject = :owner
            and (:query is null
                or lower(project.name) like lower(concat('%', :query, '%'))
                or lower(coalesce(project.description, '')) like lower(concat('%', :query, '%')))
            and (:status is null or project.status = :status)
            """)
    Page<ProjectEntity> search(@Param("owner") String owner, @Param("query") String query,
            @Param("status") String status, Pageable pageable);
}
