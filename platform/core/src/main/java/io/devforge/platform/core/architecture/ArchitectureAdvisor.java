package io.devforge.platform.core.architecture;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArchitectureAdvisor {

    public ArchitectureAssessmentResult assess(JsonNode specification) {
        JsonNode architecture = specification.path("architecture");
        JsonNode requirements = specification.path("requirements");
        JsonNode security = specification.path("security");
        JsonNode model = specification.path("model");
        String selected = architecture.path("style").asText("unspecified");
        Map<String, String> selections = selectedTechnologies(specification.path("technologySelections"));
        Map<String, String> quality = qualityPriorities(requirements.path("qualityAttributes"));
        List<ArchitectureFinding> findings = new ArrayList<>();

        boolean highSecurity = high(quality, "security");
        boolean highAvailability = high(quality, "availability");
        boolean highScalability = high(quality, "scalability");
        boolean highMaintainability = high(quality, "maintainability");
        boolean highCostPressure = high(quality, "cost");
        boolean messaging = hasMessaging(selections);
        boolean observability = hasObservability(selections);
        int entityCount = model.path("entities").size();
        int contextCount = model.path("boundedContexts").size();
        String authentication = security.path("authentication").asText("none");

        if (highSecurity && "none".equals(authentication)) {
            findings.add(finding("security-authentication-missing", "BLOCKED", "critical",
                    "High-priority security requirements exist but no application authentication strategy is selected.",
                    "Security controls must be designed before a project can be considered ready.",
                    List.of("$.security.authentication", "$.requirements.qualityAttributes"),
                    List.of("Select OIDC, OAuth2, SAML, JWT, session, or another approved authentication strategy.",
                            "Define authorization, MFA, audit, and secret-management requirements.")));
        }
        if (security.path("mfaRequired").asBoolean(false)
                && !"oidc".equals(authentication) && !"saml".equals(authentication)
                && !"oauth2".equals(authentication)) {
            findings.add(finding("mfa-provider-capability", "WARNING", "high",
                    "MFA is required but the selected authentication mode does not declare an MFA-capable identity provider.",
                    "MFA enforcement normally belongs to the identity provider and its authentication policy.",
                    List.of("$.security.mfaRequired", "$.security.identityProvider"),
                    List.of("Select an identity provider and define its MFA policy and recovery flow.")));
        }
        if (highAvailability && !observability) {
            findings.add(finding("availability-observability-gap", "WARNING", "high",
                    "High availability is a priority but no observability technology is selected.",
                    "Availability objectives need measurable health, latency, saturation, and failure signals.",
                    List.of("$.requirements.qualityAttributes", "$.technologySelections"),
                    List.of("Select metrics, logs, traces, health checks, and alerting capabilities.",
                            "Define SLOs, error budgets, and recovery objectives.")));
        }
        if ("event-driven".equals(selected) && !messaging) {
            findings.add(finding("event-driven-without-broker", "BLOCKED", "critical",
                    "Event-driven architecture is selected without a messaging or event-broker technology.",
                    "An event-driven design needs an explicit transport, delivery, retention, and failure strategy.",
                    List.of("$.architecture.style", "$.technologySelections"),
                    List.of("Select a supported broker such as Kafka or RabbitMQ.",
                            "Define event schemas, delivery guarantees, retries, DLQ, and idempotency.")));
        }
        if ("event-driven".equals(selected) && !"outbox-inbox".equals(
                architecture.path("transactionStrategy").asText("unspecified"))
                && !"saga-orchestration".equals(architecture.path("transactionStrategy").asText("unspecified"))
                && !"saga-choreography".equals(architecture.path("transactionStrategy").asText("unspecified"))) {
            findings.add(finding("event-driven-transaction-strategy", "WARNING", "high",
                    "Event-driven architecture has no explicit outbox/inbox or Saga transaction strategy.",
                    "Publishing events outside the local transaction can create lost or duplicated business events.",
                    List.of("$.architecture.transactionStrategy"),
                    List.of("Choose outbox/inbox for reliable publication and define idempotent consumers.",
                            "Use Saga only where a business transaction spans multiple services.")));
        }
        if ("microservices".equals(selected) && contextCount < 2 && entityCount < 10) {
            findings.add(finding("microservices-boundary-evidence", "WARNING", "medium",
                    "Microservices are selected but the model does not yet show multiple bounded contexts.",
                    "Service boundaries should follow business capabilities and independent change patterns, not technical layers.",
                    List.of("$.architecture.style", "$.model.boundedContexts", "$.model.entities"),
                    List.of("Identify bounded contexts and ownership boundaries.",
                            "Compare a modular monolith before accepting distributed operational cost.")));
        }
        if ("microservices".equals(selected) && highCostPressure) {
            findings.add(finding("microservices-cost-pressure", "WARNING", "medium",
                    "Microservices may conflict with a high-priority cost constraint.",
                    "Independent deployment and scaling add infrastructure, networking, observability, and on-call costs.",
                    List.of("$.architecture.style", "$.requirements.qualityAttributes"),
                    List.of("Estimate total operating cost and team ownership before splitting services.")));
        }
        if (highScalability && "layered".equals(selected)) {
            findings.add(finding("scalability-boundaries", "WARNING", "medium",
                    "Scalability is a high-priority attribute but the selected architecture has no explicit module boundaries.",
                    "A layered structure can become tightly coupled as the domain and team grow.",
                    List.of("$.architecture.style", "$.architecture.moduleBoundaries"),
                    List.of("Define bounded modules and dependency rules; consider a modular monolith before microservices.")));
        }
        if (requirements.path("qualityAttributes").isEmpty()) {
            findings.add(finding("quality-attributes-missing", "INFO", "low",
                    "No quality attributes have been defined.",
                    "Architecture recommendations are more reliable when quality goals are measurable.",
                    List.of("$.requirements.qualityAttributes"),
                    List.of("Add measurable targets for availability, performance, security, scalability, and cost.")));
        }

        String recommended = recommend(selected, highMaintainability, highScalability, contextCount, entityCount);
        String rationale = recommendationRationale(recommended, selected, contextCount, entityCount);
        List<String> tradeOffs = tradeOffs(recommended);
        findings.sort(Comparator.comparing(ArchitectureFinding::id));
        boolean review = findings.stream().anyMatch(f -> !"INFO".equals(f.status()));
        ArchitectureDecisionDraft adr = new ArchitectureDecisionDraft(
                "Adopt " + recommended + " architecture",
                "The project selected " + selected + " and has " + entityCount
                        + " modeled entities across " + contextCount + " bounded contexts.",
                "Use " + recommended + " as the initial architecture baseline, subject to owner approval.",
                List.of("Keep the selected " + selected + " style",
                        "Adopt service-based or microservices architecture",
                        "Use event-driven architecture for asynchronous business flows"),
                tradeOffs);
        return new ArchitectureAssessmentResult(selected, recommended, rationale, review, findings, tradeOffs, adr);
    }

    private Map<String, String> selectedTechnologies(JsonNode selections) {
        Map<String, String> result = new HashMap<>();
        selections.fields().forEachRemaining(entry -> {
            String id = entry.getValue().path("id").asText();
            if (!id.isBlank()) result.put(id, entry.getValue().path("version").asText());
        });
        return result;
    }

    private Map<String, String> qualityPriorities(JsonNode attributes) {
        Map<String, String> result = new HashMap<>();
        attributes.forEach(item -> result.put(item.path("attribute").asText(),
                item.path("priority").asText("medium")));
        return result;
    }

    private boolean high(Map<String, String> priorities, String key) {
        return "critical".equals(priorities.get(key)) || "high".equals(priorities.get(key));
    }

    private boolean hasMessaging(Map<String, String> selections) {
        return selections.keySet().stream().anyMatch(id -> id.startsWith("messaging.")
                || id.contains("kafka") || id.contains("rabbitmq"));
    }

    private boolean hasObservability(Map<String, String> selections) {
        return selections.keySet().stream().anyMatch(id -> id.startsWith("observability.")
                || id.contains("opentelemetry") || id.contains("prometheus") || id.contains("grafana"));
    }

    private String recommend(String selected, boolean maintainability, boolean scalability,
                             int contexts, int entities) {
        if ("event-driven".equals(selected) || "microservices".equals(selected)) {
            if (contexts < 2 && entities < 10) return "modular-monolith";
        }
        if ("layered".equals(selected) && (maintainability || scalability)) return "modular-monolith";
        if ("unspecified".equals(selected) || "custom".equals(selected)) return "modular-monolith";
        return selected;
    }

    private String recommendationRationale(String recommended, String selected, int contexts, int entities) {
        if (!recommended.equals(selected)) {
            return "The current model has " + entities + " entities and " + contexts
                    + " bounded contexts. A modular monolith preserves clear boundaries without introducing distributed operations prematurely.";
        }
        return "The selected style is retained because the current specification does not provide enough evidence to justify a different architecture. Validate the decision against measurable quality attributes.";
    }

    private List<String> tradeOffs(String style) {
        return switch (style) {
            case "modular-monolith" -> List.of(
                    "Keeps deployment and transactions simple while enforcing module boundaries.",
                    "Scales the application as a unit; module extraction requires later work.",
                    "Requires dependency rules and ownership discipline to prevent a big ball of mud.");
            case "microservices" -> List.of(
                    "Enables independent deployment and scaling of well-bounded services.",
                    "Introduces distributed failure modes, network latency, eventual consistency, and operational overhead.",
                    "Requires service ownership, observability, automation, and mature platform operations.");
            case "event-driven" -> List.of(
                    "Decouples producers and consumers and supports asynchronous workflows.",
                    "Introduces eventual consistency, replay, ordering, duplicate delivery, and schema-evolution concerns.",
                    "Requires broker operations, idempotent consumers, and reliable event publication.");
            case "layered" -> List.of(
                    "Simple to understand and effective for small, straightforward applications.",
                    "Cross-cutting changes can create coupling between layers.",
                    "Explicit module boundaries may be needed as the domain grows.");
            default -> List.of(
                    "Evaluate complexity against the team's operational maturity.",
                    "Document consistency, failure handling, deployment, and ownership trade-offs.");
        };
    }

    private ArchitectureFinding finding(String id, String status, String severity, String message,
            String rationale, List<String> paths, List<String> remediation) {
        return new ArchitectureFinding(id, status, severity, message, rationale, paths, remediation);
    }
}
