package io.devforge.platform.core.catalog;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CatalogSchemaValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TechnologyCatalogService catalog;
    private final JsonSchema technologySchema;
    private final JsonSchema compatibilitySchema;

    public CatalogSchemaValidator(TechnologyCatalogService catalog) {
        this.catalog = catalog;
        this.technologySchema = loadSchema("technology-catalog/technology.schema.json");
        this.compatibilitySchema = loadSchema("compatibility/compatibility-rule.schema.json");
    }

    public List<String> validateAll() {
        List<String> errors = new ArrayList<>();
        catalog.findTechnologies(null).forEach(entry ->
                validateEntry(entry, technologySchema, "technology", errors));
        catalog.findCompatibilityRules().forEach(entry ->
                validateEntry(entry, compatibilitySchema, "compatibility rule", errors));
        return List.copyOf(errors);
    }

    private void validateEntry(
            Map<String, Object> entry,
            JsonSchema schema,
            String type,
            List<String> errors) {
        JsonNode instance = objectMapper.valueToTree(entry);
        Set<ValidationMessage> messages = schema.validate(instance);
        messages.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .forEach(message -> errors.add(type + " " + entry.get("id") + ": " + message));
    }

    private JsonSchema loadSchema(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load catalog schema: " + path, exception);
        }
    }
}
