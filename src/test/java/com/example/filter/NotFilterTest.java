package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotFilterTest {

    @Test
    void invertsChildMatch() {
        Filter filter = Filter.not(Filter.equalTo("role", "administrator"));
        assertFalse(filter.matches(Map.of("role", "administrator")));
        assertTrue(filter.matches(Map.of("role", "user")));
    }

    @Test
    void matchesResourceMissingThePropertyEntirely() {
        // Documented consequence of 2-valued logic (DESIGN.md Decision 2): NOT(equals) on a
        // missing property evaluates to true, since equals itself evaluates to false when absent.
        Filter filter = Filter.not(Filter.equalTo("role", "administrator"));
        assertTrue(filter.matches(Map.of("age", "30")));
    }
}
