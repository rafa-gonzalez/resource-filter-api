package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleanLiteralFilterTest {

    @Test
    void alwaysTrueMatchesAnyResource() {
        assertTrue(Filter.alwaysTrue().matches(Map.of()));
        assertTrue(Filter.alwaysTrue().matches(Map.of("role", "administrator")));
    }

    @Test
    void alwaysFalseMatchesNoResource() {
        assertFalse(Filter.alwaysFalse().matches(Map.of()));
        assertFalse(Filter.alwaysFalse().matches(Map.of("role", "administrator")));
    }
}
