package io.devforge.platform.core.generation;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Component
public class GeneratedFileValidator {
    static final int MAX_FILES = 5_000;
    static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;
    private static final String RESERVED_MANIFEST = ".devforge/manifest.json";

    public Map<String, String> validate(Map<String, String> generatedFiles) {
        if (generatedFiles == null || generatedFiles.isEmpty()) {
            throw rejected("Generator produced no files.");
        }
        if (generatedFiles.size() > MAX_FILES) {
            throw tooLarge("Generated project exceeds the maximum file count of " + MAX_FILES + ".");
        }

        Map<String, String> validated = new TreeMap<>();
        long totalBytes = 0;
        for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            validatePath(path);
            if (RESERVED_MANIFEST.equals(path)) {
                throw rejected("Generator output cannot override the DevForge provenance manifest.");
            }
            if (content == null) {
                throw rejected("Generated file content cannot be null: " + path);
            }
            int fileBytes = content.getBytes(StandardCharsets.UTF_8).length;
            if (fileBytes > MAX_FILE_BYTES) {
                throw tooLarge("Generated file exceeds the 10 MiB limit: " + path);
            }
            totalBytes += fileBytes;
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw tooLarge("Generated project exceeds the 50 MiB uncompressed size limit.");
            }
            validated.put(path, content);
        }
        return Collections.unmodifiableMap(validated);
    }

    private void validatePath(String path) {
        if (path == null || path.isBlank() || path.length() > 240
                || path.startsWith("/") || path.startsWith("\\")
                || path.contains("\\") || path.contains(":")) {
            throw rejected("Unsafe generated file path.");
        }
        if (path.equals(".") || path.equals("..") || path.equals(".env")
                || path.endsWith("/.env") || path.contains("/../") || path.startsWith("../")) {
            throw rejected("Unsafe generated file path: " + path);
        }
        if (path.matches("(^|/)\\.env\\.(?!example$)[^/]+$")) {
            throw rejected("Generated environment secret files are not allowed: " + path);
        }
        String[] segments = path.split("/", -1);
        if (segments.length > 16) {
            throw rejected("Generated file path is too deeply nested: " + path);
        }
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw rejected("Unsafe generated file path: " + path);
            }
            for (int i = 0; i < segment.length(); i++) {
                if (Character.isISOControl(segment.charAt(i))) {
                    throw rejected("Generated file path contains control characters.");
                }
            }
        }
    }

    private ResponseStatusException rejected(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private ResponseStatusException tooLarge(String message) {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
