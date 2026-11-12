package io.devforge.platform.core.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.catalog.CompatibilityEvaluationResult;
import io.devforge.platform.core.catalog.CompatibilityEvaluator;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import io.devforge.platform.core.project.SpecificationSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GenerationService {
    private final SpecificationSnapshotRepository snapshots;
    private final CompatibilityEvaluator compatibilityEvaluator;
    private final JavaSpringPostgresGenerator generator;
    private final GenerationRunRepository generationRuns;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerationService(SpecificationSnapshotRepository snapshots,
            CompatibilityEvaluator compatibilityEvaluator, JavaSpringPostgresGenerator generator,
            GenerationRunRepository generationRuns) {
        this.snapshots = snapshots;
        this.compatibilityEvaluator = compatibilityEvaluator;
        this.generator = generator;
        this.generationRuns = generationRuns;
    }

    @Transactional
    public GenerationArtifact generate(UUID projectId, int snapshotVersion) throws JsonProcessingException {
        if (snapshotVersion < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "snapshotVersion must be positive");
        SpecificationSnapshotEntity snapshot = snapshots
                .findByProject_IdAndSnapshotVersion(projectId, snapshotVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Specification snapshot not found"));
        JsonNode spec = mapper.readTree(snapshot.getSpecificationJson());
        Map<String, String> selections = new HashMap<>();
        spec.path("technologySelections").fields().forEachRemaining(entry -> {
            JsonNode selection = entry.getValue();
            String id = selection.path("id").asText();
            String version = selection.path("version").asText();
            if (!id.isBlank() && !version.isBlank()) selections.put(id, version);
        });
        CompatibilityEvaluationResult evaluation = compatibilityEvaluator.evaluate(selections);
        if (!evaluation.compatible() || evaluation.requiresReview()) {
            String findings = evaluation.findings().stream()
                    .map(f -> f.ruleId() + ": " + f.message())
                    .reduce((a, b) -> a + "; " + b).orElse("Compatibility could not be confirmed");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Generation blocked by compatibility findings: " + findings);
        }

        Map<String, String> files = new TreeMap<>(generator.generate(spec));
        Map<String, Object> manifest = new java.util.LinkedHashMap<>();
        manifest.put("manifestVersion", "1.0.0");
        manifest.put("generatorId", JavaSpringPostgresGenerator.ID);
        manifest.put("generatorVersion", JavaSpringPostgresGenerator.VERSION);
        manifest.put("projectId", projectId.toString());
        manifest.put("snapshotId", snapshot.getId().toString());
        manifest.put("snapshotVersion", snapshot.getSnapshotVersion());
        manifest.put("specificationVersion", snapshot.getSchemaVersion());
        manifest.put("specificationSha256", snapshot.getContentSha256());
        manifest.put("generatedFiles", files.keySet());
        files.put(".devforge/manifest.json",
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n");

        byte[] archive = zip(files);
        String digest = sha256(archive);
        UUID runId = UUID.randomUUID();
        generationRuns.saveAndFlush(new GenerationRunEntity(runId, snapshot.getProject(), snapshot,
                JavaSpringPostgresGenerator.ID, JavaSpringPostgresGenerator.VERSION,
                "GENERATED", digest, files.size()));
        String slug = spec.path("project").path("name").asText("devforge-project")
                .toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) slug = "devforge-project";
        return new GenerationArtifact(runId, slug + ".zip", archive, digest, files.size());
    }

    private byte[] zip(Map<String, String> files) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.setLevel(9);
            for (Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                entry.setTime(0L);
                zip.putNextEntry(entry);
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to package generated project", exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
