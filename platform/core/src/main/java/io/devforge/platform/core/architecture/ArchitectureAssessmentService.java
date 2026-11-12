package io.devforge.platform.core.architecture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import io.devforge.platform.core.project.SpecificationSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ArchitectureAssessmentService {
    private final SpecificationSnapshotRepository snapshots;
    private final ArchitectureAdvisor advisor;
    private final ObjectMapper mapper = new ObjectMapper();

    public ArchitectureAssessmentService(SpecificationSnapshotRepository snapshots, ArchitectureAdvisor advisor) {
        this.snapshots = snapshots;
        this.advisor = advisor;
    }

    @Transactional(readOnly = true)
    public ArchitectureAssessmentResult evaluate(UUID projectId, int snapshotVersion)
            throws JsonProcessingException {
        if (snapshotVersion < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "snapshotVersion must be positive");
        SpecificationSnapshotEntity snapshot = snapshots
                .findByProject_IdAndSnapshotVersion(projectId, snapshotVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Specification snapshot not found"));
        JsonNode spec = mapper.readTree(snapshot.getSpecificationJson());
        return advisor.assess(spec);
    }
}
