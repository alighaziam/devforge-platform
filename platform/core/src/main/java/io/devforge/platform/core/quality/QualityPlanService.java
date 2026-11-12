package io.devforge.platform.core.quality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import io.devforge.platform.core.project.SpecificationSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class QualityPlanService {
    private final SpecificationSnapshotRepository snapshots;
    private final ObjectMapper mapper = new ObjectMapper();

    public QualityPlanService(SpecificationSnapshotRepository snapshots) {
        this.snapshots = snapshots;
    }

    @Transactional(readOnly = true)
    public QualityPlanResult createPlan(UUID projectId, int snapshotVersion) throws JsonProcessingException {
        if (snapshotVersion < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshotVersion must be positive");
        }
        SpecificationSnapshotEntity snapshot = snapshots
                .findByProject_IdAndSnapshotVersion(projectId, snapshotVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Specification snapshot not found"));
        JsonNode spec = mapper.readTree(snapshot.getSpecificationJson());
        List<QualityTestCase> cases = new ArrayList<>();
        functionalCases(spec.path("requirements").path("functional"), cases);
        modelCases(spec.path("model"), cases);
        qualityAttributeCases(spec.path("requirements").path("qualityAttributes"), cases);
        constraintCases(spec.path("requirements").path("constraints"), cases);
        securityCases(spec.path("security"), cases);
        cases.sort(Comparator.comparing(QualityTestCase::category)
                .thenComparing(QualityTestCase::id));

        Map<String, Integer> summary = new TreeMap<>();
        for (QualityTestCase testCase : cases) {
            summary.merge(testCase.category(), 1, Integer::sum);
        }
        return new QualityPlanResult(projectId, snapshot.getId(), snapshotVersion,
                Instant.now(), summary, cases, sha256(mapper.writeValueAsBytes(cases)));
    }

    private void functionalCases(JsonNode requirements, List<QualityTestCase> cases) {
        for (int requirementIndex = 0; requirementIndex < requirements.size(); requirementIndex++) {
            JsonNode requirement = requirements.get(requirementIndex);
            String id = safeId(requirement.path("id").asText("REQ"));
            String statement = requirement.path("statement").asText();
            String priority = requirement.path("priority").asText("should");
            String requirementPath = "$.requirements.functional[" + requirementIndex + "]";
            JsonNode criteria = requirement.path("acceptanceCriteria");
            if (!criteria.isArray() || criteria.isEmpty()) {
                add(cases, "FUNCTIONAL", "TEST-FR-" + (requirementIndex + 1) + "-" + id,
                        "Verify " + id, statement, priority, requirementPath,
                        List.of("Prepare the required business preconditions.",
                                "Execute the stated requirement.",
                                "Observe the resulting business state."),
                        "The application satisfies the requirement statement without violating its constraints.");
            } else {
                for (int criterionIndex = 0; criterionIndex < criteria.size(); criterionIndex++) {
                    String criterion = criteria.get(criterionIndex).asText();
                    add(cases, "FUNCTIONAL",
                            "TEST-FR-" + (requirementIndex + 1) + "-" + id + "-AC-" + (criterionIndex + 1),
                            "Verify acceptance criterion " + (criterionIndex + 1) + " for " + id,
                            criterion, priority, requirementPath + ".acceptanceCriteria[" + criterionIndex + "]",
                            List.of("Arrange the criterion's preconditions.",
                                    "Execute the required user or system action.",
                                    "Assert the observable outcome."),
                            "Acceptance criterion is met: " + criterion);
                }
            }
        }
    }

    private void modelCases(JsonNode model, List<QualityTestCase> cases) {
        JsonNode entities = model.path("entities");
        for (int entityIndex = 0; entityIndex < entities.size(); entityIndex++) {
            JsonNode entity = entities.get(entityIndex);
            String entityName = entity.path("name").asText("Entity");
            String entityId = safeId(entityName);
            String entityPath = "$.model.entities[" + entityIndex + "]";
            String entityCaseId = "TEST-DATA-ENTITY-" + (entityIndex + 1) + "-" + entityId;
            add(cases, "DATA_MODEL", entityCaseId + "-CREATE", "Create a valid " + entityName,
                    "Verify the entity can be created using valid values for its modeled attributes.",
                    "must", entityPath,
                    List.of("Construct a record with valid values for all attributes.",
                            "Persist the record through the application boundary.",
                            "Read it back and compare persisted values."),
                    "The entity is persisted and can be read back without data loss.");
            JsonNode attributes = entity.path("attributes");
            for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
                JsonNode attribute = attributes.get(attributeIndex);
                String attributeName = attribute.path("name").asText("attribute");
                String attributeId = safeId(attributeName);
                String attributePath = entityPath + ".attributes[" + attributeIndex + "]";
                if (attribute.path("required").asBoolean(false)) {
                    add(cases, "DATA_VALIDATION",
                            entityCaseId + "-" + (attributeIndex + 1) + "-" + attributeId + "-REQUIRED",
                            "Reject missing " + attributeName,
                            "Verify the required attribute is enforced.", "must", attributePath,
                            List.of("Create an otherwise valid " + entityName + " without " + attributeName + ".",
                                    "Submit the record through the application boundary."),
                            "The operation is rejected with a clear validation error.");
                }
                if (attribute.path("unique").asBoolean(false)) {
                    add(cases, "DATA_INTEGRITY",
                            entityCaseId + "-" + (attributeIndex + 1) + "-" + attributeId + "-UNIQUE",
                            "Enforce uniqueness of " + attributeName,
                            "Verify duplicate values cannot violate the uniqueness constraint.",
                            "must", attributePath,
                            List.of("Persist one " + entityName + " with a known " + attributeName + ".",
                                    "Attempt to persist a second record with the same value."),
                            "The duplicate is rejected and the original record remains unchanged.");
                }
            }
        }

        JsonNode relationships = model.path("relationships");
        for (int relationshipIndex = 0; relationshipIndex < relationships.size(); relationshipIndex++) {
            JsonNode relationship = relationships.get(relationshipIndex);
            String name = relationship.path("name").asText("relationship");
            String from = relationship.path("from").asText("source");
            String to = relationship.path("to").asText("target");
            String cardinality = relationship.path("cardinality").asText("unspecified");
            add(cases, "DATA_RELATIONSHIP",
                    "TEST-REL-" + (relationshipIndex + 1) + "-" + safeId(name),
                    "Verify relationship " + name,
                    "Verify " + from + " to " + to + " follows " + cardinality + " cardinality.",
                    "must", "$.model.relationships[" + relationshipIndex + "]",
                    List.of("Create valid source and target records.",
                            "Create the relationship through the application boundary.",
                            "Attempt an invalid association that violates the declared cardinality.",
                            "Verify the persisted association and referential integrity."),
                    "The relationship enforces " + cardinality + " cardinality and valid references.");
        }

        JsonNode indexes = model.path("indexes");
        for (int indexNumber = 0; indexNumber < indexes.size(); indexNumber++) {
            JsonNode index = indexes.get(indexNumber);
            String name = index.path("name").asText("declared-index");
            add(cases, "DATA_PERFORMANCE", "TEST-INDEX-" + (indexNumber + 1) + "-" + safeId(name),
                    "Verify declared index " + name,
                    "Verify the database migration creates the declared index and representative queries can use it.",
                    "should", "$.model.indexes[" + indexNumber + "]",
                    List.of("Apply database migrations to a clean database.",
                            "Inspect the database catalog for the index.",
                            "Run a representative query and inspect its query plan."),
                    "The index exists and the representative query plan can use it when appropriate.");
        }
    }

    private void qualityAttributeCases(JsonNode attributes, List<QualityTestCase> cases) {
        for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
            JsonNode attribute = attributes.get(attributeIndex);
            String key = attribute.path("attribute").asText("quality");
            String target = attribute.path("target").asText("a measurable target");
            String measure = attribute.path("measure").asText("the agreed project measurement");
            String priority = attribute.path("priority").asText("medium");
            String title;
            String objective;
            List<String> steps;
            String expected;
            switch (key) {
                case "performance" -> {
                    title = "Measure performance against target";
                    objective = "Measure response time and throughput against " + target + " using " + measure + ".";
                    steps = List.of("Prepare representative workload and test data.",
                            "Execute a repeatable load profile.",
                            "Capture latency percentiles, throughput, and resource saturation.");
                    expected = "Measured performance meets the agreed target.";
                }
                case "availability" -> {
                    title = "Verify availability and recovery";
                    objective = "Verify health signals and recovery behavior against " + target + ".";
                    steps = List.of("Start the application and verify readiness.",
                            "Inject a controlled dependency failure.",
                            "Observe detection, recovery, and alert behavior.");
                    expected = "The service recovers within the agreed objective and emits actionable signals.";
                }
                case "scalability" -> {
                    title = "Verify horizontal scalability";
                    objective = "Measure behavior as workload and instance count change against " + target + ".";
                    steps = List.of("Run a baseline load.",
                            "Increase concurrency and instance count.",
                            "Compare throughput, latency, and resource use.");
                    expected = "The system scales within the agreed capacity and latency envelope.";
                }
                case "security" -> {
                    title = "Verify application security controls";
                    objective = "Verify authentication, authorization, input handling, and audit controls.";
                    steps = List.of("Test unauthenticated and unauthorized access.",
                            "Exercise input validation and common injection vectors.",
                            "Verify security-relevant actions produce audit evidence.");
                    expected = "Access is denied when unauthorized and security controls behave as specified.";
                }
                case "usability" -> {
                    title = "Verify usability and accessibility";
                    objective = "Verify critical user journeys and accessibility against " + target + ".";
                    steps = List.of("Execute key user journeys with keyboard-only navigation.",
                            "Inspect labels, focus order, contrast, and screen-reader semantics.",
                            "Verify responsive layouts at supported breakpoints.");
                    expected = "Critical journeys are usable and meet the agreed accessibility criteria.";
                }
                case "observability" -> {
                    title = "Verify telemetry coverage";
                    objective = "Verify metrics, logs, traces, and alerts support diagnosis.";
                    steps = List.of("Trigger a representative successful request.",
                            "Trigger a controlled failure.",
                            "Correlate logs, traces, metrics, and alert events.");
                    expected = "Signals are emitted with correlation context and support root-cause analysis.";
                }
                default -> {
                    title = "Verify " + key;
                    objective = "Verify the quality attribute " + key + " against " + target + " using " + measure + ".";
                    steps = List.of("Define representative acceptance scenarios.",
                            "Execute scenarios in a controlled environment.",
                            "Record evidence and compare results with the target.");
                    expected = "The measured result satisfies the approved quality target.";
                }
            }
            add(cases, "QUALITY_" + key.toUpperCase(Locale.ROOT),
                    "TEST-QA-" + (attributeIndex + 1) + "-" + safeId(key),
                    title, objective, priority,
                    "$.requirements.qualityAttributes[" + attributeIndex + "]", steps, expected);
        }
    }

    private void constraintCases(JsonNode constraints, List<QualityTestCase> cases) {
        for (int index = 0; index < constraints.size(); index++) {
            String constraint = constraints.get(index).asText();
            add(cases, "CONSTRAINT", "TEST-CONSTRAINT-" + (index + 1),
                    "Verify project constraint " + (index + 1), constraint, "must",
                    "$.requirements.constraints[" + index + "]",
                    List.of("Prepare a scenario that exercises the constraint.",
                            "Execute the relevant workflow.",
                            "Inspect the resulting behavior and configuration."),
                    "The implementation complies with the stated constraint: " + constraint);
        }
    }

    private void securityCases(JsonNode security, List<QualityTestCase> cases) {
        String authentication = security.path("authentication").asText("none");
        if (!"none".equals(authentication)) {
            add(cases, "SECURITY", "TEST-SEC-AUTHN", "Verify authentication policy",
                    "Verify the configured authentication strategy rejects invalid credentials and accepts valid identities.",
                    "must", "$.security.authentication",
                    List.of("Attempt access without credentials.",
                            "Attempt access with invalid credentials.",
                            "Authenticate with a valid identity and verify the principal."),
                    "Only valid authenticated identities receive access.");
        }
        if (security.path("mfaRequired").asBoolean(false)) {
            add(cases, "SECURITY", "TEST-SEC-MFA", "Verify multi-factor authentication",
                    "Verify MFA is required for the configured authentication flow.",
                    "must", "$.security.mfaRequired",
                    List.of("Start an authentication flow with valid primary credentials.",
                            "Attempt to complete the flow without the second factor.",
                            "Complete the flow with a valid second factor."),
                    "Authentication cannot complete without the required second factor.");
        }
        JsonNode authorizationModels = security.path("authorizationModels");
        for (int index = 0; index < authorizationModels.size(); index++) {
            String model = authorizationModels.get(index).asText();
            add(cases, "SECURITY", "TEST-SEC-AUTHZ-" + (index + 1) + "-" + safeId(model),
                    "Verify " + model.toUpperCase(Locale.ROOT) + " authorization",
                    "Verify access decisions enforce the configured " + model + " authorization model.",
                    "must", "$.security.authorizationModels[" + index + "]",
                    List.of("Create identities with different authorization attributes.",
                            "Exercise allowed and denied resource operations.",
                            "Verify policy changes take effect according to the configured policy."),
                    "Only operations permitted by the configured authorization model are allowed.");
        }
        if (security.path("auditRequired").asBoolean(false)) {
            add(cases, "SECURITY", "TEST-SEC-AUDIT", "Verify security audit trail",
                    "Verify security-relevant and business-critical operations produce protected audit events.",
                    "must", "$.security.auditRequired",
                    List.of("Execute successful and denied security-sensitive operations.",
                            "Inspect audit events for actor, action, target, timestamp, and outcome.",
                            "Verify audit records cannot be modified by ordinary application users."),
                    "Required audit events are complete, attributable, and protected against unauthorized changes.");
        }
    }

    private void add(List<QualityTestCase> cases, String category, String id, String title,
            String objective, String priority, String sourcePath, List<String> steps, String expected) {
        cases.add(new QualityTestCase(id, category, title, objective, priority, sourcePath, steps, expected));
    }

    private String safeId(String value) {
        String result = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return result.isBlank() ? "ITEM" : result;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
