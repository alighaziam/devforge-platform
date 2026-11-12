package io.devforge.platform.core.specification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Service
public class ProjectSpecificationValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchema schema = loadSchema();

    public SpecificationValidationResult validate(String json) throws JsonProcessingException {
        JsonNode instance = objectMapper.readTree(json);
        Set<ValidationMessage> messages = schema.validate(instance);
        if (messages.isEmpty()) {
            return SpecificationValidationResult.success();
        }

        List<String> errors = messages.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .toList();
        return SpecificationValidationResult.failure(errors);
    }

    private JsonSchema loadSchema() {
        ClassPathResource resource =
                new ClassPathResource("project-specification/project-specification.schema.json");
        try (InputStream input = resource.getInputStream()) {
            JsonSchemaFactory factory =
                    JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            return factory.getSchema(input);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load the Project Specification JSON Schema", exception);
        }
    }
}
