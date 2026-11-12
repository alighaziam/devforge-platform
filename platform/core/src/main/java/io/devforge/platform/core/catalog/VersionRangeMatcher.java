package io.devforge.platform.core.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionRangeMatcher {

    private static final Pattern VERSION = Pattern.compile("^\\d+(?:\\.\\d+)*$");
    private static final Pattern CONSTRAINT =
            Pattern.compile("^(>=|<=|>|<|=)?(\\d+(?:\\.\\d+)*)$");

    private VersionRangeMatcher() {
    }

    static Optional<Boolean> matches(String version, String range) {
        List<Integer> actual = parseVersion(version);
        if (actual == null || range == null || range.isBlank()) {
            return Optional.empty();
        }

        String[] constraints = range.trim().split("\\s+");
        for (String constraint : constraints) {
            Matcher matcher = CONSTRAINT.matcher(constraint);
            if (!matcher.matches()) {
                return Optional.empty();
            }
            String operator = matcher.group(1) == null ? "=" : matcher.group(1);
            List<Integer> expected = parseVersion(matcher.group(2));
            if (expected == null) {
                return Optional.empty();
            }

            int comparison = compare(actual, expected);
            boolean accepted = switch (operator) {
                case ">=" -> comparison >= 0;
                case "<=" -> comparison <= 0;
                case ">" -> comparison > 0;
                case "<" -> comparison < 0;
                case "=" -> comparison == 0;
                default -> false;
            };
            if (!accepted) {
                return Optional.of(false);
            }
        }
        return Optional.of(true);
    }

    private static List<Integer> parseVersion(String value) {
        if (value == null || !VERSION.matcher(value).matches()) {
            return null;
        }
        List<Integer> parts = new ArrayList<>();
        try {
            for (String part : value.split("\\.")) {
                parts.add(Integer.parseInt(part));
            }
            return parts;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int compare(List<Integer> left, List<Integer> right) {
        int length = Math.max(left.size(), right.size());
        for (int index = 0; index < length; index++) {
            int a = index < left.size() ? left.get(index) : 0;
            int b = index < right.size() ? right.get(index) : 0;
            int result = Integer.compare(a, b);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
