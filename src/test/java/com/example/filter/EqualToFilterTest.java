package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EqualToFilterTest {

    @Test
    void matchesExactValue() {
        Filter filter = Filter.equalTo("role", "administrator");
        assertTrue(filter.matches(Map.of("role", "administrator")));
    }

    @Test
    void matchesCaseInsensitively() {
        Filter filter = Filter.equalTo("role", "Administrator");
        assertTrue(filter.matches(Map.of("role", "ADMINISTRATOR")));
    }

    @Test
    void doesNotMatchDifferentValue() {
        Filter filter = Filter.equalTo("role", "administrator");
        assertFalse(filter.matches(Map.of("role", "user")));
    }

    @Test
    void doesNotMatchMissingProperty() {
        Filter filter = Filter.equalTo("role", "administrator");
        assertFalse(filter.matches(Map.of("age", "30")));
    }

    @Test
    void doesNotMatchEmptyProperty() {
        Filter filter = Filter.equalTo("role", "administrator");
        assertFalse(filter.matches(Map.of("role", "  ")));
    }

    @Test
    void treatsNumericValuesWithDifferentFormattingAsEqual() {
        Filter filter = Filter.equalTo("age", "035");
        assertTrue(filter.matches(Map.of("age", "35")));
    }

    @Test
    void trimsWhitespaceBeforeComparing() {
        Filter filter = Filter.equalTo("role", "administrator");
        assertTrue(filter.matches(Map.of("role", "  administrator  ")));
    }

    @Test
    void propertyNamesAreCaseSensitive() {
        // The spec states property names are case-sensitive while values are not, so a filter on
        // "Role" must not pick up the resource's "role" entry.
        Filter filter = Filter.equalTo("Role", "administrator");
        assertFalse(filter.matches(Map.of("role", "administrator")));
    }

    @Test
    void trimsTheFiltersOwnTargetValue() {
        Filter filter = Filter.equalTo("role", "  administrator  ");
        assertTrue(filter.matches(Map.of("role", "administrator")));
    }

    @Test
    void doesNotTreatDecimalValuesAsNumeric() {
        // DESIGN.md Decision 1: only integers get numeric-aware comparison. "5.0" is not an
        // integer, so this falls back to strict string comparison against "5" and does not match,
        // even though they're mathematically equal. Asserted explicitly so it reads as intended.
        Filter filter = Filter.equalTo("price", "5.0");
        assertFalse(filter.matches(Map.of("price", "5")));
    }
}
