package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreaterThanFilterTest {

    @Test
    void matchesNumericallyGreaterValue() {
        Filter filter = Filter.greaterThan("age", "30");
        assertTrue(filter.matches(Map.of("age", "35")));
    }

    @Test
    void doesNotMatchNumericallyLesserOrEqualValue() {
        Filter filter = Filter.greaterThan("age", "30");
        assertFalse(filter.matches(Map.of("age", "30")));
        assertFalse(filter.matches(Map.of("age", "25")));
    }

    @Test
    void fallsBackToLexicographicForNonNumericValues() {
        Filter filter = Filter.greaterThan("name", "banana");
        assertTrue(filter.matches(Map.of("name", "cherry")));
        assertFalse(filter.matches(Map.of("name", "apple")));
    }

    @Test
    void doesNotMatchMissingProperty() {
        Filter filter = Filter.greaterThan("age", "30");
        assertFalse(filter.matches(Map.of("role", "administrator")));
    }
}
