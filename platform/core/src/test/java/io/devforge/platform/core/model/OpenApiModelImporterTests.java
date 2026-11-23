package io.devforge.platform.core.model;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiModelImporterTests {

    private final OpenApiModelImporter importer = new OpenApiModelImporter();

    @Test
    void importsOpenApi31ObjectSchemasPrimitiveAttributesAndRelationships() throws Exception {
        String document = """
                {
                  "openapi": "3.1.0",
                  "info": {"title": "Orders API", "version": "1.0.0"},
                  "paths": {"/orders": {"get": {"summary": "List orders"}}},
                  "components": {
                    "schemas": {
                      "Customer": {
                        "type": "object",
                        "required": ["id", "email"],
                        "properties": {
                          "id": {"type": "string", "format": "uuid"},
                          "email": {"type": "string", "format": "email"},
                          "profile": {"type": "object", "properties": {"timezone": {"type": "string"}}}
                        }
                      },
                      "Order": {
                        "type": "object",
                        "required": ["id", "customer"],
                        "properties": {
                          "id": {"type": "string", "format": "uuid"},
                          "customer": {"$ref": "#/components/schemas/Customer"},
                          "items": {"type": "array", "items": {"$ref": "#/components/schemas/OrderItem"}}
                        }
                      },
                      "OrderItem": {
                        "type": "object",
                        "properties": {
                          "sku": {"type": "string"},
                          "quantity": {"type": "integer", "format": "int32"}
                        }
                      }
                    }
                  }
                }
                """;

        OpenApiModelImporter.ImportResult result = importer.importOpenApi(document);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) result.model().get("entities");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relationships = (List<Map<String, Object>>) result.model().get("relationships");

        assertThat(entities).extracting(entity -> entity.get("name"))
                .containsExactly("Customer", "Order", "OrderItem");
        Map<String, Object> customer = entities.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> customerAttributes = (List<Map<String, Object>>) customer.get("attributes");
        assertThat(customerAttributes).anySatisfy(attribute -> {
            assertThat(attribute.get("name")).isEqualTo("id");
            assertThat(attribute.get("type")).isEqualTo("uuid");
            assertThat(attribute.get("required")).isEqualTo(true);
        });
        assertThat(customerAttributes).anySatisfy(attribute -> {
            assertThat(attribute.get("name")).isEqualTo("profile");
            assertThat(attribute.get("type")).isEqualTo("json");
        });

        assertThat(relationships).hasSize(2);
        assertThat(relationships).anySatisfy(relationship -> {
            assertThat(relationship.get("name")).isEqualTo("customer");
            assertThat(relationship.get("from")).isEqualTo("Order");
            assertThat(relationship.get("to")).isEqualTo("Customer");
            assertThat(relationship.get("cardinality")).isEqualTo("many-to-one");
            assertThat(relationship.get("optional")).isEqualTo(false);
        });
        assertThat(relationships).anySatisfy(relationship -> {
            assertThat(relationship.get("name")).isEqualTo("items");
            assertThat(relationship.get("cardinality")).isEqualTo("one-to-many");
            assertThat(relationship.get("optional")).isEqualTo(true);
        });
        assertThat(result.model().get("indexes")).isEqualTo(List.of());
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("paths"));
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("JSON attributes"));
    }

    @Test
    void mapsOpenApi31NullableTypesToOptionalAttributes() throws Exception {
        String document = """
                {
                  "openapi": "3.1.0",
                  "components": {
                    "schemas": {
                      "Profile": {
                        "type": "object",
                        "required": ["displayName"],
                        "properties": {
                          "displayName": {"type": ["string", "null"]},
                          "age": {"type": "integer", "format": "int64"}
                        }
                      }
                    }
                  }
                }
                """;

        OpenApiModelImporter.ImportResult result = importer.importOpenApi(document);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) result.model().get("entities");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) entities.get(0).get("attributes");

        assertThat(attributes).anySatisfy(attribute -> {
            assertThat(attribute.get("name")).isEqualTo("displayName");
            assertThat(attribute.get("required")).isEqualTo(false);
        });
        assertThat(attributes).anySatisfy(attribute -> {
            assertThat(attribute.get("name")).isEqualTo("age");
            assertThat(attribute.get("type")).isEqualTo("long");
        });
    }

    @Test
    void rejectsOpenApiV2MissingSchemasExternalRefsAndComposition() {
        assertThatThrownBy(() -> importer.importOpenApi("""
                {"swagger":"2.0","definitions":{}}
                """)).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> importer.importOpenApi("""
                {"openapi":"3.0.3","components":{"schemas":{}}}
                """)).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> importer.importOpenApi("""
                {"openapi":"3.0.3","components":{"schemas":{"Order":{"type":"object",
                  "properties":{"customer":{"$ref":"https://example.com/customer.json"}}}}}}
                """)).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> importer.importOpenApi("""
                {"openapi":"3.1.0","components":{"schemas":{"Order":{"allOf":[
                  {"type":"object","properties":{"id":{"type":"string"}}}]}}}}
                """)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsUnknownLocalReferencesAndUnsafeNameCollisions() {
        assertThatThrownBy(() -> importer.importOpenApi("""
                {"openapi":"3.0.3","components":{"schemas":{"Order":{"type":"object",
                  "properties":{"customer":{"$ref":"#/components/schemas/Missing"}}}}}}
                """)).isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> importer.importOpenApi("""
                {"openapi":"3.0.3","components":{"schemas":{
                  "Customer-Profile":{"type":"object","properties":{}},
                  "Customer_Profile":{"type":"object","properties":{}}
                }}}
                """)).isInstanceOf(ResponseStatusException.class);
    }
}
