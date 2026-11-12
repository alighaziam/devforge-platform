package io.devforge.platform.core.generation;

import java.time.Instant;
import java.util.UUID;

public record GenerationRunView(
        UUID id,
        UUID projectId,
        UUID snapshotId,
        int snapshotVersion,
        String generatorId,
        String generatorVersion,
        String status,
        String outputSha256,
        int generatedFileCount,
        Instant createdAt) {

    static GenerationRunView from(GenerationRunEntity entity) {
        return new GenerationRunView(
                entity.getId(),
                entity.getProject().getId(),
                entity.getSnapshot().getId(),
                entity.getSnapshot().getSnapshotVersion(),
                entity.getGeneratorId(),
                entity.getGeneratorVersion(),
                entity.getStatus(),
                entity.getOutputSha256(),
                entity.getGeneratedFileCount(),
                entity.getCreatedAt());
    }
}
