package com.example.filter;

import java.util.Map;
import java.util.Objects;

abstract class AbstractPropertyFilter implements Filter {

    private final String property;
    private final String targetValue;
    private final Integer targetNumericValue;

    AbstractPropertyFilter(String property, String value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        this.property = property;
        this.targetValue = value.trim();
        this.targetNumericValue = tryParseInt(this.targetValue);
    }

    @Override
    public final boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource must not be null");

        String resourceValue = resource.get(this.property);
        if (resourceValue == null) {
            return false;
        }

        String trimmed = resourceValue.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // Numeric if both sides parse as integers, else case-insensitive lexicographic.
        int comparisonResult;
        if (this.targetNumericValue != null) {
            Integer resourceNumericValue = tryParseInt(trimmed);
            comparisonResult = (resourceNumericValue != null)
                    ? Integer.compare(resourceNumericValue, this.targetNumericValue)
                    : trimmed.compareToIgnoreCase(this.targetValue);
        } else {
            comparisonResult = trimmed.compareToIgnoreCase(this.targetValue);
        }

        return matchesComparison(comparisonResult);
    }

    /**
     * Reduces to a single sign check per subclass ({@code == 0}, {@code < 0}, {@code > 0}) since
     * {@code compareToIgnoreCase(...) == 0} is equivalent to {@code equalsIgnoreCase(...)} — this
     * is what lets equals share the same comparison shape as the ordering operators.
     *
     * @param comparisonResult sign of (resource value) vs. (target value) — numeric if both are
     *                         integers, else case-insensitive lexicographic
     */
    protected abstract boolean matchesComparison(int comparisonResult);

    protected abstract String operator();

    @Override
    public final String toString() {
        if (this.targetNumericValue != null) {
            return this.property + " " + operator() + " " + this.targetValue;
        }
        return this.property + " " + operator() + " '" + this.targetValue + "'";
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
