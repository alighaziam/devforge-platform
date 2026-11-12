package io.devforge.platform.core.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSchemaValidatorTests {

    private final CatalogSchemaValidator validator =
            new CatalogSchemaValidator(new TechnologyCatalogService());

    @Test
    void allCheckedInCatalogRecordsConformToTheirVersionedSchemas() {
        assertThat(validator.validateAll()).isEmpty();
    }
}
