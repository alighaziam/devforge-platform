package io.devforge.platform.core.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureAdvisorTests {
    private final ArchitectureAdvisor advisor = new ArchitectureAdvisor();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void blocksHighSecurityRequirementsWithoutAuthentication() throws Exception {
        var spec = mapper.readTree("""
                {
                  "architecture": { "style": "modular-monolith", "decisions": [] },
                  "requirements": { "qualityAttributes": [
                    { "attribute": "security", "priority": "critical" }
                  ] },
                  "security": { "authentication": "none" },
                  "model": { "entities": [], "boundedContexts": [] },
                  "technologySelections": {}
                }
                """);

        ArchitectureAssessmentResult result = advisor.assess(spec);

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.findings()).extracting(ArchitectureFinding::id)
                .contains("security-authentication-missing");
    }

    @Test
    void recommendsModularMonolithWhenEventDrivenHasNoBrokerOrDomainBoundaries() throws Exception {
        var spec = mapper.readTree("""
                {
                  "architecture": { "style": "event-driven", "decisions": [],
                    "transactionStrategy": "unspecified" },
                  "requirements": { "qualityAttributes": [] },
                  "model": { "entities": [], "boundedContexts": [] },
                  "technologySelections": {}
                }
                """);

        ArchitectureAssessmentResult result = advisor.assess(spec);

        assertThat(result.recommendedStyle()).isEqualTo("modular-monolith");
        assertThat(result.findings()).extracting(ArchitectureFinding::id)
                .contains("event-driven-without-broker", "quality-attributes-missing");
    }

    @Test
    void producesTradeOffsAndAnAdrDraft() throws Exception {
        var spec = mapper.readTree("""
                {
                  "architecture": { "style": "layered", "decisions": [] },
                  "requirements": { "qualityAttributes": [
                    { "attribute": "maintainability", "priority": "high" }
                  ] },
                  "model": { "entities": [], "boundedContexts": [] },
                  "technologySelections": {}
                }
                """);

        ArchitectureAssessmentResult result = advisor.assess(spec);

        assertThat(result.recommendedStyle()).isEqualTo("modular-monolith");
        assertThat(result.tradeOffs()).isNotEmpty();
        assertThat(result.adrDraft().title()).contains("modular-monolith");
    }
}
