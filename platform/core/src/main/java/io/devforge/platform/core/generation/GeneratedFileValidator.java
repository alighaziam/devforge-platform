package io.devforge.platform.core.generation;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class GeneratedFileValidator {

    static final int MAX_FILES = 5_000;
    static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    private static final String RESERVED_MANIFEST = ".devforge/manifest.json";

    public void validate(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            throw rejected("Generator returned no files.");
        }
        if (files.size() > MAX_FILES) {
            throw rejected("Generator output exceeds the " + MAX_FILES + " file limit.");
        }

        Set<String> portablePaths = new HashSet<>();
        long totalBytes = 0;
        for (Map.Entry<String, String> file : files.entrySet()) {
            String path = file.getKey();
            String content = file.getValue();
            validatePath(path, portablePaths);
            if (RESERVED_MANIFEST.equals(path)) {
                throw rejected("Generator output cannot replace the reserved DevForge manifest.");
            }
            if (content == null) {
                throw rejected("Generated file content cannot be null: " + path);
            }

            int fileBytes = content.getBytes(StandardCharsets.UTF_8).length;
            if (fileBytes > MAX_FILE_BYTES) {
                throw rejected("Generated file exceeds the 10 MiB limit: " + path);
            }
            totalBytes += fileBytes;
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw rejected("Generator output exceeds the 100 MiB total size limit.");
            }
        }
    }

    private void validatePath(String path, Set<String> portablePaths) {
        if (path == null || path.isBlank() || path.length() > 240
                || path.startsWith("/") || path.startsWith("\\")
                || path.matches("^[A-Za-z]:.*") || path.contains("\\")
                || path.chars().anyMatch(character -> character < 32 || character == 127)) {
            throw rejected("Generator returned an unsafe artifact path.");
        }

        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)
                    || segment.endsWith(".") || segment.endsWith(" ")) {
                throw rejected("Generator returned an unsafe artifact path: " + path);
            }
            if (".git".equalsIgnoreCase(segment)) {
                throw rejected("Generator output cannot write into a .git directory.");
            }
        }

        String fileName = segments[segments.length - 1];
        if (".env".equalsIgnoreCase(fileName)
                || (fileName.toLowerCase(Locale.ROOT).startsWith(".env.")
                    && !".env.example".equalsIgnoreCase(fileName))) {
            throw rejected("Generator output cannot contain environment secret files: " + path);
        }

        String portablePath = path.toLowerCase(Locale.ROOT);
        if (!portablePaths.add(portablePath)) {
            throw rejected("Generator returned duplicate paths that collide on case-insensitive filesystems: " + path);
        }
    }

    private ResponseStatusException rejected(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
