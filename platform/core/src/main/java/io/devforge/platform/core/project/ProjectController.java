package io.devforge.platform.core.project;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectLifecycleService projects;

    public ProjectController(ProjectLifecycleService projects) {
        this.projects = projects;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectLifecycleService.ProjectView create(@RequestBody CreateProjectRequest request) {
        return projects.create(request.name(), request.description());
    }

    @GetMapping("/{id}")
    public ProjectLifecycleService.ProjectView get(@PathVariable UUID id) {
        return projects.get(id);
    }

    @GetMapping
    public Page<ProjectLifecycleService.ProjectView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status) {
        return projects.list(page, size, query, status);
    }

    @PutMapping("/{id}")
    public ProjectLifecycleService.ProjectView update(@PathVariable UUID id,
                                                      @RequestBody UpdateProjectRequest request) {
        return projects.update(id, request.name(), request.description(), request.version());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projects.delete(id);
    }

    public record CreateProjectRequest(String name, String description) { }
    public record UpdateProjectRequest(String name, String description, long version) { }
}
