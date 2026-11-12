package io.devforge.platform.core.specification;

import java.util.List;

public record SpecificationValidationResult(boolean valid, List<String> errors) {

    public SpecificationValidationResult {
        errors = List.copyOf(errors);
    }

    public static SpecificationValidationResult success() {
        return new SpecificationValidationResult(true, List.of());
    }

    public static SpecificationValidationResult failure(List<String> errors) {
        return new SpecificationValidationResult(false, errors);
    }
}
