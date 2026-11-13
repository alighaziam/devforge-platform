package io.devforge.platform.core.project;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Enforces project ownership for every nested project API, including modules
 * whose controllers are maintained independently (generation, architecture,
 * quality and model import).
 */
@Component
public class ProjectAccessInterceptor implements HandlerInterceptor {

    private static final String PROJECT_PREFIX = "/api/v1/projects/";
    private final ProjectLifecycleService projects;

    public ProjectAccessInterceptor(ProjectLifecycleService projects) {
        this.projects = projects;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(PROJECT_PREFIX)) {
            return true;
        }
        String remainder = path.substring(PROJECT_PREFIX.length());
        String idSegment = remainder.split("/", 2)[0];
        try {
            projects.get(UUID.fromString(idSegment));
            return true;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
    }
}
