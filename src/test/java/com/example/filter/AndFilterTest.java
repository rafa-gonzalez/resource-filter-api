package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AndFilterTest {

    @Test
    void matchesWhenAllChildrenMatch() {
        Filter filter = Filter.and(Filter.equalTo("role", "administrator"), Filter.greaterThan("age", "30"));
        assertTrue(filter.matches(Map.of("role", "administrator", "age", "35")));
    }

    @Test
    void doesNotMatchWhenAnyChildFails() {
        Filter filter = Filter.and(Filter.equalTo("role", "administrator"), Filter.greaterThan("age", "30"));
        assertFalse(filter.matches(Map.of("role", "administrator", "age", "25")));
    }

    @Test
    void singleChildBehavesLikeThatChild() {
        // One child is legal (the floor is one, not two) and evaluates exactly like the child.
        Filter filter = Filter.and(Filter.equalTo("role", "administrator"));
        assertTrue(filter.matches(Map.of("role", "administrator")));
        assertFalse(filter.matches(Map.of("role", "user")));
    }

    @Test
    void rejectsZeroChildren() {
        assertThrows(IllegalArgumentException.class, () -> Filter.and());
    }
}
