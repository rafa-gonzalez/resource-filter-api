package com.example.filter;

import java.util.Map;
import java.util.Objects;

abstract class AbstractPropertyFilter implements Filter {

    final String property;
    final String rawValue;
    final Integer numericValue;

    AbstractPropertyFilter(String property, String value) {
        Objects.requireNonNull(property, "property must not be null");
        Objects.requireNonNull(value, "value must not be null");
        this.property = property;
        this.rawValue = value.trim();
        this.numericValue = tryParseInt(this.rawValue);
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

        int comparisonResult;
        if (this.numericValue != null) {
            Integer resourceInt = tryParseInt(trimmed);
            comparisonResult = (resourceInt != null)
                    ? Integer.compare(resourceInt, this.numericValue)
                    : trimmed.compareToIgnoreCase(this.rawValue);
        } else {
            comparisonResult = trimmed.compareToIgnoreCase(this.rawValue);
        }

        return matchesComparison(comparisonResult);
    }

    /**
     * @param comparisonResult sign of (resource value) compared to (this filter's target value) —
     *                          numeric comparison if both parsed as integers, else case-insensitive
     *                          lexicographic comparison
     */
    protected abstract boolean matchesComparison(int comparisonResult);

    protected abstract String operator();

    @Override
    public final String toString() {
        if (this.numericValue != null) {
            return this.property + " " + operator() + " " + this.rawValue;
        }
        return this.property + " " + operator() + " '" + this.rawValue + "'";
    }

    static Integer tryParseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
