package io.devforge.platform.core.catalog;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityEvaluatorTests {

    private final CompatibilityEvaluator evaluator =
            new CompatibilityEvaluator(new TechnologyCatalogService());

    @Test
    void acceptsSupportedSpringBootAndJavaSelection() {
        CompatibilityEvaluationResult result = evaluator.evaluate(Map.of(
                "framework.spring-boot", "4.0.0",
                "language.java", "25"));

        assertThat(result.compatible()).isTrue();
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void blocksMissingRequiredJavaRuntime() {
        CompatibilityEvaluationResult result = evaluator.evaluate(Map.of(
                "framework.spring-boot", "4.0.0"));

        assertThat(result.compatible()).isFalse();
        assertThat(result.findings())
                .extracting(CompatibilityFinding::status)
                .contains("BLOCKED");
    }

    @Test
    void blocksUnsupportedJavaRuntime() {
        CompatibilityEvaluationResult result = evaluator.evaluate(Map.of(
                "framework.spring-boot", "4.0.0",
                "language.java", "11"));

        assertThat(result.compatible()).isFalse();
        assertThat(result.findings())
                .extracting(CompatibilityFinding::status)
                .contains("BLOCKED");
    }

    @Test
    void marksUnknownTechnologyAsIndeterminateRatherThanCompatible() {
        CompatibilityEvaluationResult result = evaluator.evaluate(Map.of(
                "unknown.runtime", "1.0"));

        assertThat(result.compatible()).isFalse();
        assertThat(result.requiresReview()).isTrue();
        assertThat(result.findings())
                .extracting(CompatibilityFinding::status)
                .contains("INDETERMINATE");
    }
}
