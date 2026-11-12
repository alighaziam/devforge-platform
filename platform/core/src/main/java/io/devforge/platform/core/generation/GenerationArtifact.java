package io.devforge.platform.core.generation;

public record GenerationArtifact(String fileName, byte[] content, String sha256, int generatedFiles) {
    public GenerationArtifact {
        content = content.clone();
    }
    @Override
    public byte[] content() {
        return content.clone();
    }
}
