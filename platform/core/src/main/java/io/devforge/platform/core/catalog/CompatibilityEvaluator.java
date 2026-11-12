package io.devforge.platform.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CompatibilityEvaluator {

    private final TechnologyCatalogService catalog;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompatibilityEvaluator(TechnologyCatalogService catalog) {
        this.catalog = catalog;
    }

    public CompatibilityEvaluationResult evaluate(Map<String, String> selections) {
        Map<String, String> safeSelections = selections == null ? Map.of() : Map.copyOf(selections);
        List<CompatibilityFinding> findings = new ArrayList<>();

        evaluateUnknownSelections(safeSelections, findings);
        evaluateDependencies(safeSelections, findings);
        evaluateConflicts(safeSelections, findings);
        evaluateRules(safeSelections, findings);

        boolean compatible = findings.stream().noneMatch(finding ->
                "BLOCKED".equals(finding.status()) || "INDETERMINATE".equals(finding.status()));
        boolean requiresReview = findings.stream().anyMatch(finding ->
                "WARNING".equals(finding.status()) || "INDETERMINATE".equals(finding.status()));

        return new CompatibilityEvaluationResult(compatible, requiresReview, findings);
    }

    private void evaluateUnknownSelections(
            Map<String, String> selections,
            List<CompatibilityFinding> findings) {
        selections.forEach((technologyId, version) -> {
            if (catalog.findTechnology(technologyId) == null) {
                findings.add(finding(
                        "unknown-technology-" + technologyId,
                        "INDETERMINATE",
                        "error",
                        "Selected technology is not present in the catalog: " + technologyId,
                        "The compatibility engine cannot verify an unknown catalog entry.",
                        List.of(technologyId),
                        List.of("Add a reviewed catalog record or select a known technology."),
                        List.of(),
                        "never"));
            }
        });
    }

    private void evaluateDependencies(
            Map<String, String> selections,
            List<CompatibilityFinding> findings) {
        selections.forEach((technologyId, selectedVersion) -> {
            Map<String, Object> technology = catalog.findTechnology(technologyId);
            if (technology == null) {
                return;
            }
            Object dependencies = technology.get("requires");
            if (!(dependencies instanceof List<?> list)) {
                return;
            }
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> dependency)) {
                    continue;
                }
                String dependencyId = stringValue(dependency.get("technologyId"));
                String versionRange = stringValue(dependency.get("versionRange"));
                boolean optional = Boolean.TRUE.equals(dependency.get("optional"));
                String dependencyVersion = selections.get(dependencyId);
                if (dependencyVersion == null) {
                    if (!optional) {
                        findings.add(finding(
                                "missing-dependency-" + technologyId + "-" + dependencyId,
                                "BLOCKED",
                                "error",
                                technologyId + " requires " + dependencyId + " " + versionRange,
                                stringValue(dependency.get("reason")),
                                List.of(technologyId, dependencyId),
                                List.of("Select " + dependencyId + " with a version in range " + versionRange + "."),
                                List.of(),
                                "never"));
                    }
                    continue;
                }

                Optional<Boolean> matches = VersionRangeMatcher.matches(dependencyVersion, versionRange);
                if (matches.isEmpty()) {
                    findings.add(finding(
                            "unknown-dependency-version-" + technologyId + "-" + dependencyId,
                            "INDETERMINATE",
                            "error",
                            "Cannot evaluate version range " + versionRange + " for " + dependencyId,
                            "The catalog range or selected version uses a format not supported by the current evaluator.",
                            List.of(technologyId, dependencyId),
                            List.of("Use numeric dot-separated versions and supported comparison operators, or extend the version scheme implementation."),
                            List.of(),
                            "never"));
                } else if (!matches.get()) {
                    findings.add(finding(
                            "incompatible-dependency-" + technologyId + "-" + dependencyId,
                            "BLOCKED",
                            "error",
                            technologyId + " requires " + dependencyId + " " + versionRange + " but " + dependencyVersion + " was selected.",
                            stringValue(dependency.get("reason")),
                            List.of(technologyId, dependencyId),
                            List.of("Select a compatible " + dependencyId + " version."),
                            List.of(),
                            "never"));
                }
            }
        });
    }

    private void evaluateConflicts(
            Map<String, String> selections,
            List<CompatibilityFinding> findings) {
        selections.forEach((technologyId, selectedVersion) -> {
            Map<String, Object> technology = catalog.findTechnology(technologyId);
            if (technology == null || !(technology.get("conflicts") instanceof List<?> conflicts)) {
                return;
            }
            for (Object item : conflicts) {
                if (!(item instanceof Map<?, ?> conflict)) {
                    continue;
                }
                String conflictId = stringValue(conflict.get("technologyId"));
                if (selections.containsKey(conflictId)) {
                    findings.add(finding(
                            "technology-conflict-" + technologyId + "-" + conflictId,
                            "BLOCKED",
                            "error",
                            technologyId + " conflicts with " + conflictId + ".",
                            stringValue(conflict.get("reason")),
                            List.of(technologyId, conflictId),
                            List.of("Remove one of the conflicting technology selections."),
                            List.of(),
                            "never"));
                }
            }
        });
    }

    private void evaluateRules(
            Map<String, String> selections,
            List<CompatibilityFinding> findings) {
        for (Map<String, Object> rule : catalog.findCompatibilityRules()) {
            String ruleId = stringValue(rule.get("id"));
            JsonNode ruleNode = objectMapper.valueToTree(rule);
            Match applies = appliesTo(ruleNode.path("appliesTo"), selections);
            if (applies == Match.FALSE) {
                continue;
            }
            if (applies == Match.UNKNOWN) {
                findings.add(ruleFinding(ruleNode, "INDETERMINATE",
                        "Unable to determine whether the selected versions match the rule scope."));
                continue;
            }

            Match condition = evaluateExpression(ruleNode.path("condition"), selections);
            if (condition == Match.TRUE) {
                String effect = ruleNode.path("effect").asText();
                String status = switch (effect) {
                    case "block" -> "BLOCKED";
                    case "warn" -> "WARNING";
                    case "recommend" -> "RECOMMENDATION";
                    default -> "INDETERMINATE";
                };
                findings.add(ruleFinding(ruleNode, status, null));
            } else if (condition == Match.UNKNOWN) {
                findings.add(ruleFinding(ruleNode, "INDETERMINATE",
                        "The rule condition could not be evaluated with the available catalog data."));
            }
        }
    }

    private Match appliesTo(JsonNode selectors, Map<String, String> selections) {
        boolean unknown = false;
        for (JsonNode selector : selectors) {
            String technologyId = selector.path("technologyId").asText();
            String selectedVersion = selections.get(technologyId);
            if (selectedVersion == null) {
                continue;
            }
            Optional<Boolean> matches = VersionRangeMatcher.matches(
                    selectedVersion, selector.path("versionRange").asText());
            if (matches.isEmpty()) {
                unknown = true;
            } else if (matches.get()) {
                return Match.TRUE;
            }
        }
        return unknown ? Match.UNKNOWN : Match.FALSE;
    }

    private Match evaluateExpression(JsonNode expression, Map<String, String> selections) {
        if (expression.has("allOf")) {
            boolean unknown = false;
            for (JsonNode child : expression.path("allOf")) {
                Match result = evaluateExpression(child, selections);
                if (result == Match.FALSE) return Match.FALSE;
                if (result == Match.UNKNOWN) unknown = true;
            }
            return unknown ? Match.UNKNOWN : Match.TRUE;
        }
        if (expression.has("anyOf")) {
            boolean unknown = false;
            for (JsonNode child : expression.path("anyOf")) {
                Match result = evaluateExpression(child, selections);
                if (result == Match.TRUE) return Match.TRUE;
                if (result == Match.UNKNOWN) unknown = true;
            }
            return unknown ? Match.UNKNOWN : Match.FALSE;
        }
        if (expression.has("not")) {
            return evaluateExpression(expression.path("not"), selections).negate();
        }

        String operator = expression.path("operator").asText();
        String technologyId = expression.path("technologyId").asText();
        return switch (operator) {
            case "selected" -> selections.containsKey(technologyId) ? Match.TRUE : Match.FALSE;
            case "version-in-range" -> {
                String selectedVersion = selections.get(technologyId);
                if (selectedVersion == null) yield Match.FALSE;
                Optional<Boolean> result = VersionRangeMatcher.matches(
                        selectedVersion, expression.path("versionRange").asText());
                yield result.map(value -> value ? Match.TRUE : Match.FALSE).orElse(Match.UNKNOWN);
            }
            case "provides-capability" -> {
                Map<String, Object> technology = catalog.findTechnology(technologyId);
                if (technology == null) yield Match.UNKNOWN;
                Object capabilities = technology.get("capabilities");
                if (!(capabilities instanceof List<?> values)) yield Match.FALSE;
                yield values.contains(expression.path("capability").asText())
                        ? Match.TRUE : Match.FALSE;
            }
            default -> Match.UNKNOWN;
        };
    }

    private CompatibilityFinding ruleFinding(JsonNode rule, String status, String detail) {
        String message = rule.path("message").asText();
        if (detail != null) message = detail + " " + message;
        List<String> affected = new ArrayList<>();
        rule.path("appliesTo").forEach(selector ->
                affected.add(selector.path("technologyId").asText()));
        List<String> remediation = strings(rule.path("remediation"));
        List<String> evidence = strings(rule.path("evidence"));
        return new CompatibilityFinding(
                rule.path("id").asText(),
                status,
                rule.path("severity").asText(),
                message,
                rule.path("rationale").asText(),
                affected,
                remediation,
                evidence,
                rule.path("overridePolicy").asText());
    }

    private CompatibilityFinding finding(
            String id, String status, String severity, String message, String rationale,
            List<String> affected, List<String> remediation, List<String> evidence,
            String overridePolicy) {
        return new CompatibilityFinding(id, status, severity, message, rationale,
                affected, remediation, evidence, overridePolicy);
    }

    private List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private enum Match {
        TRUE, FALSE, UNKNOWN;

        Match negate() {
            return switch (this) {
                case TRUE -> FALSE;
                case FALSE -> TRUE;
                case UNKNOWN -> UNKNOWN;
            };
        }
    }
}
