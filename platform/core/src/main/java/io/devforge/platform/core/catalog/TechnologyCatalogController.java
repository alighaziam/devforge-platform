package io.devforge.platform.core.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
public class TechnologyCatalogController {

    private final TechnologyCatalogService catalogService;

    public TechnologyCatalogController(TechnologyCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/technologies")
    public List<Map<String, Object>> technologies(
            @RequestParam(required = false) String category) {
        return catalogService.findTechnologies(category);
    }

    @GetMapping("/technologies/{id}")
    public Map<String, Object> technology(@PathVariable String id) {
        Map<String, Object> technology = catalogService.findTechnology(id);
        if (technology == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Technology not found");
        }
        return technology;
    }

    @GetMapping("/compatibility-rules")
    public List<Map<String, Object>> compatibilityRules() {
        return catalogService.findCompatibilityRules();
    }
}
