package io.devforge.platform.core.specification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectSpecificationControllerTests {

    private ProjectSpecificationController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectSpecificationController(
                new ProjectSpecificationValidator(new ObjectMapper()));
    }

    @Test
    void returnsBadRequestForMalformedJson() {
        ResponseEntity<?> response = controller.validate("{invalid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsPayloadsLargerThanOneMebibyteBeforeParsing() {
        ResponseEntity<?> response = controller.validate(" ".repeat(1_048_577));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
