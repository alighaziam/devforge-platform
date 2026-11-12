package io.devforge.platform.core.plugin;

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
public class PluginManifestValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchema schema = loadSchema();

    public PluginManifestValidationResult validate(String json) throws JsonProcessingException {
        JsonNode instance = objectMapper.readTree(json);
        Set<ValidationMessage> messages = schema.validate(instance);
        List<String> errors = messages.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .toList();
        return new PluginManifestValidationResult(errors.isEmpty(), errors);
    }

    private JsonSchema loadSchema() {
        try (InputStream input = new ClassPathResource(
                "plugin-manifest/plugin-manifest.schema.json").getInputStream()) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the Plugin Manifest schema", exception);
        }
    }
}
