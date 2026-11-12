package io.devforge.platform.core.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnologyCatalogServiceTests {

    private final TechnologyCatalogService catalog = new TechnologyCatalogService();

    @Test
    void loadsEveryIndexedTechnologyAndRule() {
        assertThat(catalog.findTechnologies(null)).hasSize(5);
        assertThat(catalog.findCompatibilityRules()).hasSize(1);
    }

    @Test
    void filtersTechnologiesByCategory() {
        assertThat(catalog.findTechnologies("database"))
                .extracting(entry -> entry.get("id"))
                .containsExactly("database.postgresql");
    }

    @Test
    void resolvesTechnologyByStableId() {
        assertThat(catalog.findTechnology("language.java"))
                .containsEntry("version", "25");
    }

    @Test
    void returnsNullForUnknownTechnology() {
        assertThat(catalog.findTechnology("unknown.technology")).isNull();
    }
}
