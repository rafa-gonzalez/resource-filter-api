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
    void fallsBackToLexicographicWhenOnlyTheTargetIsNumeric() {
        // Numeric comparison needs BOTH sides to parse as integers. Here the target is numeric but
        // the resource value isn't, so it degrades to case-insensitive text comparison:
        // "abc" sorts after "30".
        Filter filter = Filter.greaterThan("age", "30");
        assertTrue(filter.matches(Map.of("age", "abc")));
    }

    @Test
    void comparesLexicographicallyIgnoringCase() {
        Filter filter = Filter.greaterThan("name", "Banana");
        assertTrue(filter.matches(Map.of("name", "CHERRY")));
        assertFalse(filter.matches(Map.of("name", "APPLE")));
    }

    @Test
    void comparesNegativeIntegersNumerically() {
        Filter filter = Filter.greaterThan("temp", "-5");
        assertTrue(filter.matches(Map.of("temp", "0")));
        assertFalse(filter.matches(Map.of("temp", "-10")));
    }

    @Test
    void doesNotMatchMissingProperty() {
        Filter filter = Filter.greaterThan("age", "30");
        assertFalse(filter.matches(Map.of("role", "administrator")));
    }
}
