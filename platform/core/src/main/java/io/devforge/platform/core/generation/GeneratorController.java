package io.devforge.platform.core.generation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/generators")
public class GeneratorController {

    private final GeneratorRegistry registry;

    public GeneratorController(GeneratorRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<GeneratorDescriptor> list() {
        return registry.descriptors();
    }
}
