package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessThanFilterTest {

    @Test
    void matchesNumericallyLesserValue() {
        Filter filter = Filter.lessThan("age", "30");
        assertTrue(filter.matches(Map.of("age", "25")));
    }

    @Test
    void doesNotMatchNumericallyGreaterOrEqualValue() {
        Filter filter = Filter.lessThan("age", "30");
        assertFalse(filter.matches(Map.of("age", "30")));
        assertFalse(filter.matches(Map.of("age", "35")));
    }

    @Test
    void fallsBackToLexicographicForNonNumericValues() {
        Filter filter = Filter.lessThan("name", "banana");
        assertTrue(filter.matches(Map.of("name", "apple")));
        assertFalse(filter.matches(Map.of("name", "cherry")));
    }

    @Test
    void doesNotMatchMissingProperty() {
        Filter filter = Filter.lessThan("age", "30");
        assertFalse(filter.matches(Map.of("role", "administrator")));
    }
}
