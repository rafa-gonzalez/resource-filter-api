package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrFilterTest {

    @Test
    void matchesWhenAnyChildMatches() {
        Filter filter = Filter.or(Filter.equalTo("role", "administrator"), Filter.equalTo("role", "superadmin"));
        assertTrue(filter.matches(Map.of("role", "superadmin")));
    }

    @Test
    void doesNotMatchWhenNoChildMatches() {
        Filter filter = Filter.or(Filter.equalTo("role", "administrator"), Filter.equalTo("role", "superadmin"));
        assertFalse(filter.matches(Map.of("role", "user")));
    }

    @Test
    void singleChildBehavesLikeThatChild() {
        Filter filter = Filter.or(Filter.equalTo("role", "administrator"));
        assertTrue(filter.matches(Map.of("role", "administrator")));
        assertFalse(filter.matches(Map.of("role", "user")));
    }

    @Test
    void rejectsZeroChildren() {
        assertThrows(IllegalArgumentException.class, () -> Filter.or());
    }
}
