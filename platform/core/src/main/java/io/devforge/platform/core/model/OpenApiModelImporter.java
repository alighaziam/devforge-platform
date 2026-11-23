package io.devforge.platform.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Imports the data-model portion of an OpenAPI 3.x JSON document.
 * Remote references are deliberately not resolved; importing is a local preview operation.
 */
@Component
public class OpenApiModelImporter {

    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");
    private final ObjectMapper mapper = new ObjectMapper();

    public ImportResult importOpenApi(String content) throws JsonProcessingException {
        if (content == null || content.isBlank()) {
            throw rejected("OpenAPI document is empty.");
        }
        JsonNode root = mapper.readTree(content);
        String version = root.path("openapi").asText("");
        if (!root.isObject() || !version.startsWith("3.")) {
            throw rejected("Only OpenAPI 3.x JSON documents are supported.");
        }

        JsonNode schemasNode = root.path("components").path("schemas");
        if (!schemasNode.isObject() || schemasNode.isEmpty()) {
            throw rejected("OpenAPI document must contain components.schemas.");
        }

        Map<String, JsonNode> schemas = new LinkedHashMap<>();
        Map<String, String> entityNames = new LinkedHashMap<>();
        Set<String> normalizedNames = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> fields = schemasNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue() == null || !field.getValue().isObject()) {
                throw rejected("OpenAPI schema definitions must be objects: " + field.getKey());
            }
            schemas.put(field.getKey(), field.getValue());
            if (field.getValue().has("properties") && !field.getValue().path("properties").isObject()) {
                throw rejected("Schema properties must be an object: " + field.getKey());
            }
            if (isObjectSchema(field.getValue())) {
                String normalized = safeName(field.getKey(), "schema");
                if (!normalizedNames.add(normalized)) {
                    throw rejected("Schema names collide after normalization: " + field.getKey());
                }
                entityNames.put(field.getKey(), normalized);
            }
        }
        if (entityNames.isEmpty()) {
            throw rejected("No object schemas were found in components.schemas.");
        }

        List<Map<String, Object>> entities = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean[] usedJsonAttributes = {false};

        for (Map.Entry<String, String> schemaEntry : entityNames.entrySet()) {
            JsonNode schema = schemas.get(schemaEntry.getKey());
            rejectComposition(schema, schemaEntry.getKey());
            String entityName = schemaEntry.getValue();
            Set<String> requiredProperties = requiredProperties(schema, schemaEntry.getKey());
            Set<String> seenPropertyNames = new LinkedHashSet<>();
            List<Map<String, Object>> attributes = new ArrayList<>();

            JsonNode properties = schema.path("properties");
            Iterator<Map.Entry<String, JsonNode>> propertyFields = properties.fields();
            while (propertyFields.hasNext()) {
                Map.Entry<String, JsonNode> property = propertyFields.next();
                String propertyName = safeName(property.getKey(), "property");
                if (!seenPropertyNames.add(propertyName)) {
                    throw rejected("Property names collide after normalization on schema " + entityName);
                }
                JsonNode definition = property.getValue();
                boolean required = requiredProperties.contains(property.getKey()) && !isNullable(definition);

                if (definition.has("$ref")) {
                    String targetOriginal = resolveReference(definition.path("$ref").asText(), schemas);
                    if (entityNames.containsKey(targetOriginal)) {
                        relationships.add(relationship(propertyName, entityName,
                                entityNames.get(targetOriginal), "many-to-one", !required));
                        continue;
                    }
                    addAttribute(attributes, propertyName, mapPrimitiveType(schemas.get(targetOriginal)),
                            required, definition);
                    continue;
                }

                if ("array".equals(schemaType(definition))) {
                    JsonNode items = definition.path("items");
                    if (items.has("$ref")) {
                        String targetOriginal = resolveReference(items.path("$ref").asText(), schemas);
                        if (entityNames.containsKey(targetOriginal)) {
                            relationships.add(relationship(propertyName, entityName,
                                    entityNames.get(targetOriginal), "one-to-many", !required));
                        } else {
                            addAttribute(attributes, propertyName, "json", required, definition);
                            usedJsonAttributes[0] = true;
                        }
                    } else {
                        addAttribute(attributes, propertyName, "json", required, definition);
                        usedJsonAttributes[0] = true;
                    }
                    continue;
                }

                String type = mapPrimitiveType(definition);
                boolean jsonAttribute = "json".equals(type);
                addAttribute(attributes, propertyName, type, required, definition);
                usedJsonAttributes[0] |= jsonAttribute;
            }

            for (String required : requiredProperties) {
                if (!properties.has(required)) {
                    throw rejected("Required property is not defined on schema " + entityName + ": " + required);
                }
            }

            Map<String, Object> entity = new LinkedHashMap<>();
            entity.put("name", entityName);
            entity.put("attributes", attributes);
            if (schema.path("x-bounded-context").isTextual()) {
                entity.put("boundedContext", schema.path("x-bounded-context").asText());
            }
            entities.add(entity);
        }

        if (schemas.size() > entityNames.size()) {
            warnings.add("Scalar/enum component schemas are used as attribute types where possible and are not imported as entities.");
        }
        if (usedJsonAttributes[0]) {
            warnings.add("Inline objects and arrays without object-schema references are represented as JSON attributes.");
        }
        if (root.path("paths").isObject() && !root.path("paths").isEmpty()) {
            warnings.add("OpenAPI paths, operations, request/response bodies and security schemes are not imported; only components.schemas are mapped.");
        }
        warnings.add("External references and schema composition (allOf/oneOf/anyOf) are not resolved by this local preview importer.");

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entities", entities);
        model.put("relationships", relationships);
        model.put("indexes", List.of());
        return new ImportResult(model, warnings);
    }

    private static Map<String, Object> relationship(String name, String from, String to,
                                                     String cardinality, boolean optional) {
        Map<String, Object> relationship = new LinkedHashMap<>();
        relationship.put("name", name);
        relationship.put("from", from);
        relationship.put("to", to);
        relationship.put("cardinality", cardinality);
        relationship.put("ownership", "reference");
        relationship.put("optional", optional);
        return relationship;
    }

    private void addAttribute(List<Map<String, Object>> attributes, String name, String type,
                              boolean required, JsonNode definition) {
        Map<String, Object> attribute = new LinkedHashMap<>();
        attribute.put("name", name);
        attribute.put("type", type);
        attribute.put("required", required);
        attribute.put("unique", definition.path("x-unique").asBoolean(false));
        if (definition.path("description").isTextual()) {
            attribute.put("description", definition.path("description").asText());
        }
        if (definition.has("default")) {
            attribute.put("default", mapper.convertValue(definition.get("default"), Object.class));
        }
        attributes.add(attribute);
    }

    private static Set<String> requiredProperties(JsonNode schema, String schemaName) {
        JsonNode required = schema.path("required");
        if (required.isMissingNode()) {
            return Set.of();
        }
        if (!required.isArray()) {
            throw rejected("Schema required must be an array: " + schemaName);
        }
        Set<String> names = new LinkedHashSet<>();
        required.forEach(value -> {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw rejected("Schema required entries must be non-empty strings: " + schemaName);
            }
            names.add(value.asText());
        });
        return names;
    }

    private static boolean isObjectSchema(JsonNode schema) {
        return "object".equals(schemaType(schema)) || schema.path("properties").isObject();
    }

    private static String schemaType(JsonNode schema) {
        JsonNode type = schema.path("type");
        if (type.isTextual()) {
            return type.asText();
        }
        if (type.isArray()) {
            for (JsonNode candidate : type) {
                if (candidate.isTextual() && !"null".equals(candidate.asText())) {
                    return candidate.asText();
                }
            }
        }
        return "";
    }

    private static boolean isNullable(JsonNode schema) {
        if (schema.path("nullable").asBoolean(false)) {
            return true;
        }
        JsonNode type = schema.path("type");
        if (type.isArray()) {
            for (JsonNode candidate : type) {
                if (candidate.isTextual() && "null".equals(candidate.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String mapPrimitiveType(JsonNode schema) {
        if (schema == null) {
            throw rejected("Referenced schema was not found.");
        }
        String type = schemaType(schema);
        String format = schema.path("format").asText("").toLowerCase(Locale.ROOT);
        if ("string".equals(type)) {
            return switch (format) {
                case "uuid" -> "uuid";
                case "date" -> "date";
                case "date-time" -> "datetime";
                default -> "string";
            };
        }
        if ("integer".equals(type)) {
            return "int64".equals(format) ? "long" : "integer";
        }
        if ("number".equals(type)) {
            return "decimal";
        }
        if ("boolean".equals(type)) {
            return "boolean";
        }
        if ("object".equals(type) || "array".equals(type) || type.isBlank()) {
            return "json";
        }
        throw rejected("Unsupported OpenAPI schema type: " + type);
    }

    private static String resolveReference(String reference, Map<String, JsonNode> schemas) {
        String prefix = "#/components/schemas/";
        if (reference == null || !reference.startsWith(prefix)) {
            throw rejected("Only local #/components/schemas references are supported.");
        }
        String target = reference.substring(prefix.length())
                .replace("~1", "/")
                .replace("~0", "~");
        if (target.isBlank() || !schemas.containsKey(target)) {
            throw rejected("OpenAPI reference target does not exist: " + target);
        }
        return target;
    }

    private static String safeName(String value, String kind) {
        if (value == null || value.isBlank()) {
            throw rejected("OpenAPI " + kind + " name is empty.");
        }
        if (SAFE_NAME.matcher(value).matches()) {
            return value;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9_]", "_");
        if (!normalized.matches("[A-Za-z_].*")) {
            normalized = "Imported_" + normalized;
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        if (!SAFE_NAME.matcher(normalized).matches()) {
            throw rejected("OpenAPI " + kind + " name cannot be normalized safely: " + value);
        }
        return normalized;
    }

    private static void rejectComposition(JsonNode schema, String name) {
        if (schema.has("allOf") || schema.has("oneOf") || schema.has("anyOf") || schema.has("not")) {
            throw rejected("Schema composition is not yet supported: " + name);
        }
    }

    private static ResponseStatusException rejected(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record ImportResult(Map<String, Object> model, List<String> warnings) {
        public ImportResult {
            model = Map.copyOf(model);
            warnings = List.copyOf(warnings);
        }
    }
}
