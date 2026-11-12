package io.devforge.platform.core.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.specification.ProjectSpecificationValidator;
import io.devforge.platform.core.specification.SpecificationValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectLifecycleService {

    private final ProjectRepository projects;
    private final SpecificationSnapshotRepository snapshots;
    private final ProjectSpecificationValidator specificationValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectLifecycleService(ProjectRepository projects,
            SpecificationSnapshotRepository snapshots,
            ProjectSpecificationValidator specificationValidator) {
        this.projects = projects;
        this.snapshots = snapshots;
        this.specificationValidator = specificationValidator;
    }

    public ProjectView create(String name, String description) {
        if (name == null || name.isBlank() || name.length() > 120
                || (description != null && description.length() > 2000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project name must contain 1 to 120 characters and description must not exceed 2000 characters");
        }
        ProjectEntity entity = new ProjectEntity(UUID.randomUUID(), name.trim(),
                description == null ? null : description.trim());
        return ProjectView.from(projects.saveAndFlush(entity));
    }

    @Transactional(readOnly = true)
    public ProjectView get(UUID id) {
        return ProjectView.from(findProject(id));
    }

    @Transactional(readOnly = true)
    public Page<ProjectView> list(int page, int size) {
        return projects.findAll(PageRequest.of(Math.max(0, page),
                        Math.min(Math.max(1, size), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ProjectView::from);
    }

    public SnapshotView createSnapshot(UUID projectId, String json) throws JsonProcessingException {
        if (json.getBytes(StandardCharsets.UTF_8).length > 1_048_576) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Specification payload exceeds the 1 MiB limit");
        }
        SpecificationValidationResult validation = specificationValidator.validate(json);
        if (!validation.valid()) {
            throw new InvalidSpecificationException(validation);
        }
        JsonNode root = objectMapper.readTree(json);
        String specificationProjectId = root.path("project").path("id").asText();
        if (!projectId.toString().equals(specificationProjectId)) {
            throw new InvalidSpecificationException(new SpecificationValidationResult(
                    false, List.of("$.project.id must match the project ID in the request path.")));
        }

        ProjectEntity project = projects.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found"));
        int nextVersion = snapshots.findTopByProject_IdOrderBySnapshotVersionDesc(projectId)
                .map(snapshot -> snapshot.getSnapshotVersion() + 1)
                .orElse(1);
        SpecificationSnapshotEntity snapshot = new SpecificationSnapshotEntity(
                UUID.randomUUID(), project, nextVersion, "1.0.0", sha256(json), json);
        return SnapshotView.from(snapshots.saveAndFlush(snapshot));
    }

    @Transactional(readOnly = true)
    public List<SnapshotView> listSnapshots(UUID projectId) {
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return snapshots.findAllByProject_IdOrderBySnapshotVersionAsc(projectId)
                .stream().map(SnapshotView::from).toList();
    }

    private ProjectEntity findProject(UUID id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found"));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record ProjectView(UUID id, String name, String description, String status,
                              java.time.Instant createdAt, java.time.Instant updatedAt,
                              long version) {
        static ProjectView from(ProjectEntity entity) {
            return new ProjectView(entity.getId(), entity.getName(), entity.getDescription(),
                    entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
        }
    }

    public record SnapshotView(UUID id, UUID projectId, int snapshotVersion, String schemaVersion,
                               String contentSha256, java.time.Instant createdAt) {
        static SnapshotView from(SpecificationSnapshotEntity entity) {
            return new SnapshotView(entity.getId(), entity.getProject().getId(),
                    entity.getSnapshotVersion(), entity.getSchemaVersion(),
                    entity.getContentSha256(), entity.getCreatedAt());
        }
    }

    public static class InvalidSpecificationException extends RuntimeException {
        private final SpecificationValidationResult validation;
        InvalidSpecificationException(SpecificationValidationResult validation) {
            super("Project Specification is invalid");
            this.validation = validation;
        }
        public SpecificationValidationResult getValidation() { return validation; }
    }
}
