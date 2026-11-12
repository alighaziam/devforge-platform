package io.devforge.platform.core.specification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectSpecificationValidatorTests {

    private ProjectSpecificationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProjectSpecificationValidator(new ObjectMapper());
    }

    @Test
    void acceptsAValidMinimalProjectSpecification() throws Exception {
        String json = """
                {
                  "specVersion": "1.0.0",
                  "project": {
                    "id": "orders",
                    "name": "Orders",
                    "description": "Order management"
                  },
                  "requirements": {
                    "functional": [],
                    "qualityAttributes": [],
                    "constraints": []
                  },
                  "model": {
                    "entities": [],
                    "relationships": []
                  },
                  "architecture": {
                    "style": "modular-monolith",
                    "decisions": []
                  },
                  "technologySelections": {},
                  "generation": {
                    "profile": "recommended",
                    "outputLayout": "single-project"
                  }
                }
                """;

        SpecificationValidationResult result = validator.validate(json);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredSectionsAndUnknownProperties() throws Exception {
        String json = """
                {
                  "specVersion": "1.0.0",
                  "project": {
                    "id": "orders",
                    "name": "Orders",
                    "description": "Order management"
                  },
                  "unexpected": true
                }
                """;

        SpecificationValidationResult result = validator.validate(json);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> validator.validate("{invalid"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
}
