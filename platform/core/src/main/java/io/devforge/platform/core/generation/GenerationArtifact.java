package io.devforge.platform.core.generation;

import java.util.UUID;

public record GenerationArtifact(UUID runId, String fileName, byte[] content,
                                 String sha256, int generatedFiles) {
    public GenerationArtifact {
        content = content.clone();
    }
    @Override
    public byte[] content() {
        return content.clone();
    }
}
