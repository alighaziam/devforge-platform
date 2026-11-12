package io.devforge.platform.core.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionRangeMatcherTests {

    @Test
    void matchesMultipleNumericConstraints() {
        assertThat(VersionRangeMatcher.matches("4.1.2", ">=4.0.0 <5.0.0"))
                .contains(true);
    }

    @Test
    void rejectsVersionsOutsideRange() {
        assertThat(VersionRangeMatcher.matches("5.0.0", ">=4.0.0 <5.0.0"))
                .contains(false);
    }

    @Test
    void treatsEquivalentPaddedVersionsAsEqual() {
        assertThat(VersionRangeMatcher.matches("17.0", "=17"))
                .contains(true);
    }

    @Test
    void reportsUnsupportedVersionFormatsAsUnknown() {
        assertThat(VersionRangeMatcher.matches("25-ea", ">=17"))
                .isEmpty();
    }
}
