package io.devforge.platform.core.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/compatibility")
public class CompatibilityController {

    private static final int MAX_SELECTIONS = 100;
    private final CompatibilityEvaluator evaluator;

    public CompatibilityController(CompatibilityEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @PostMapping("/evaluate")
    public CompatibilityEvaluationResult evaluate(
            @RequestBody CompatibilityEvaluationRequest request) {
        if (request.selections().size() > MAX_SELECTIONS
                || request.selections().entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue().isBlank()
                        || entry.getKey().length() > 160 || entry.getValue().length() > 80)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selections must contain at most 100 non-empty technology/version pairs");
        }
        return evaluator.evaluate(request.selections());
    }
}
