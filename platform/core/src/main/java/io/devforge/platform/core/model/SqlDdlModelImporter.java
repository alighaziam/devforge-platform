package io.devforge.platform.core.model;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlDdlModelImporter {

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?is)^CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\"A-Za-z_][\"A-Za-z0-9_]*)\\s*\\((.*)\\)$");
    private static final Pattern CREATE_INDEX = Pattern.compile(
            "(?is)^CREATE\\s+(UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\"A-Za-z_][\"A-Za-z0-9_]*)\\s+ON\\s+([\"A-Za-z_][\"A-Za-z0-9_]*)\\s*\\((.*)\\)$");
    private static final Pattern COLUMN = Pattern.compile("(?is)^([\"A-Za-z_][\"A-Za-z0-9_]*)\\s+(.+)$");

    public Map<String, Object> importDdl(String ddl) {
        if (ddl == null || ddl.isBlank()) throw rejected("SQL DDL content is empty.");
        if (ddl.length() > 524_288) throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE, "SQL DDL exceeds the 512 KiB limit.");

        String clean = ddl.replaceAll("(?m)--.*$", "").replaceAll("(?s)/\\*.*?\\*/", "");
        List<Map<String, Object>> entities = new ArrayList<>();
        List<Map<String, Object>> indexes = new ArrayList<>();
        Map<String, Map<String, Map<String, Object>>> columnsByTable = new LinkedHashMap<>();

        for (String raw : clean.split(";")) {
            String statement = raw.trim();
            if (statement.isEmpty()) continue;
            Matcher tableMatcher = CREATE_TABLE.matcher(statement);
            Matcher indexMatcher = CREATE_INDEX.matcher(statement);
            if (tableMatcher.matches()) {
                String table = identifier(tableMatcher.group(1));
                if (columnsByTable.containsKey(table)) throw rejected("Duplicate table definition: " + table);
                Map<String, Map<String, Object>> columns = parseColumns(tableMatcher.group(2), table);
                columnsByTable.put(table, columns);
                entities.add(Map.of("name", table, "attributes", new ArrayList<>(columns.values())));
            } else if (indexMatcher.matches()) {
                String indexName = identifier(indexMatcher.group(2));
                String table = identifier(indexMatcher.group(3));
                List<String> columns = splitTopLevel(indexMatcher.group(4)).stream()
                        .map(String::trim).map(SqlDdlModelImporter::identifier).toList();
                if (columns.isEmpty()) throw rejected("Index " + indexName + " has no columns.");
                Map<String, Object> index = new LinkedHashMap<>();
                index.put("name", indexName);
                index.put("entity", table);
                index.put("columns", columns);
                index.put("unique", indexMatcher.group(1) != null);
                indexes.add(index);
            } else {
                throw rejected("Unsupported SQL statement. Only CREATE TABLE and CREATE [UNIQUE] INDEX are accepted.");
            }
        }

        if (entities.isEmpty()) throw rejected("No CREATE TABLE statements were found.");
        for (Map<String, Object> index : indexes) {
            String table = (String) index.get("entity");
            Map<String, Map<String, Object>> columns = columnsByTable.get(table);
            if (columns == null) throw rejected("Index references unknown table: " + table);
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) index.get("columns");
            for (String name : names) {
                if (!columns.containsKey(name)) throw rejected("Index references unknown column " + name + " on " + table);
            }
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entities", entities);
        model.put("relationships", List.of());
        model.put("indexes", indexes);
        return model;
    }

    private Map<String, Map<String, Object>> parseColumns(String body, String table) {
        Map<String, Map<String, Object>> columns = new LinkedHashMap<>();
        List<String> primaryKeys = new ArrayList<>();
        List<List<String>> uniqueConstraints = new ArrayList<>();

        for (String definition : splitTopLevel(body)) {
            String part = definition.trim();
            if (part.isEmpty()) continue;
            String upper = part.toUpperCase(Locale.ROOT);
            if (upper.startsWith("CONSTRAINT ")) {
                part = part.replaceFirst("(?is)^CONSTRAINT\\s+[\"A-Za-z0-9_]+\\s+", "");
                upper = part.toUpperCase(Locale.ROOT);
            }
            if (upper.startsWith("PRIMARY KEY")) {
                primaryKeys.addAll(constraintColumns(part));
                continue;
            }
            if (upper.startsWith("UNIQUE")) {
                uniqueConstraints.add(constraintColumns(part));
                continue;
            }
            if (upper.startsWith("FOREIGN KEY") || upper.startsWith("CHECK")) {
                throw rejected("Foreign-key and CHECK constraints are not yet supported by the SQL importer.");
            }

            Matcher matcher = COLUMN.matcher(part);
            if (!matcher.matches()) throw rejected("Cannot parse column definition: " + part);
            String name = identifier(matcher.group(1));
            String tail = matcher.group(2).trim();
            String upperTail = tail.toUpperCase(Locale.ROOT);
            if (upperTail.contains(" REFERENCES ")) {
                throw rejected("Foreign-key column constraints are not yet supported.");
            }
            if (upperTail.contains(" DEFAULT ")) {
                throw rejected("DEFAULT expressions are not yet supported by the SQL importer.");
            }
            String typeToken = tail.replaceAll("(?is)\\s+(NOT\\s+NULL|NULL|UNIQUE|PRIMARY\\s+KEY).*", "").trim();
            String type = modelType(typeToken);
            boolean required = upperTail.contains("NOT NULL") || upperTail.contains("PRIMARY KEY");
            boolean unique = upperTail.contains("UNIQUE") || upperTail.contains("PRIMARY KEY");
            if (upperTail.contains("PRIMARY KEY")) primaryKeys.add(name);
            Map<String, Object> attribute = new LinkedHashMap<>();
            attribute.put("name", name);
            attribute.put("type", type);
            attribute.put("required", required);
            attribute.put("unique", unique);
            if (columns.putIfAbsent(name, attribute) != null) {
                throw rejected("Duplicate column " + name + " in table " + table);
            }
        }

        if (primaryKeys.size() > 1) {
            throw rejected("Composite primary keys are not supported in this import profile.");
        }
        if (!primaryKeys.isEmpty()) {
            String key = primaryKeys.get(0);
            Map<String, Object> attribute = columns.get(key);
            if (attribute == null) throw rejected("Primary key references an unknown column: " + key);
            if (!"id".equals(key) || !"uuid".equals(attribute.get("type"))) {
                throw rejected("This import profile supports only a single UUID primary key named id.");
            }
            attribute.put("required", true);
            attribute.put("unique", true);
        }
        for (List<String> constraint : uniqueConstraints) {
            for (String name : constraint) {
                Map<String, Object> attribute = columns.get(name);
                if (attribute == null) throw rejected("Unique constraint references unknown column: " + name);
                attribute.put("unique", true);
            }
        }
        return columns;
    }

    private List<String> constraintColumns(String expression) {
        int open = expression.indexOf('(');
        int close = expression.lastIndexOf(')');
        if (open < 0 || close <= open) throw rejected("Malformed table constraint: " + expression);
        return splitTopLevel(expression.substring(open + 1, close)).stream()
                .map(String::trim).map(SqlDdlModelImporter::identifier).toList();
    }

    private List<String> splitTopLevel(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') quoted = !quoted;
            if (!quoted && c == '(') depth++;
            if (!quoted && c == ')') depth--;
            if (!quoted && depth == 0 && c == ',') {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
            if (depth < 0) throw rejected("Unbalanced parentheses in SQL DDL.");
        }
        if (depth != 0 || quoted) throw rejected("Unbalanced delimiters in SQL DDL.");
        parts.add(value.substring(start));
        return parts;
    }

    private String modelType(String raw) {
        String type = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").replaceAll("\\s*\\(.*\\)", "").trim();
        return switch (type) {
            case "uuid" -> "uuid";
            case "varchar", "character varying", "char", "character" -> "string";
            case "text" -> "text";
            case "smallint", "integer", "int", "serial" -> "integer";
            case "bigint", "bigserial" -> "long";
            case "numeric", "decimal" -> "decimal";
            case "boolean", "bool" -> "boolean";
            case "date" -> "date";
            case "timestamp", "timestamp with time zone", "timestamp without time zone", "timestamptz" -> "datetime";
            default -> throw rejected("Unsupported SQL column type: " + raw);
        };
    }

    private static String identifier(String value) {
        String result = value.trim().replace("\"", "");
        if (!result.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unsafe SQL identifier: " + value);
        }
        return result;
    }

    private ResponseStatusException rejected(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
