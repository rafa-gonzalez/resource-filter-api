package com.example.filter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositionTest {

    @Test
    void specWorkedExample() {
        Map<String, String> user = new LinkedHashMap<>();
        user.put("firstname", "Joe");
        user.put("surname", "Bloggs");
        user.put("role", "administrator");
        user.put("age", "35");

        Filter filter = Filter.and(
                Filter.equalTo("role", "administrator"),
                Filter.greaterThan("age", "30")
        );

        assertTrue(filter.matches(user));

        user.put("age", "25");
        assertFalse(filter.matches(user));
    }

    @Test
    void arbitrarilyNestedFilter() {
        Filter filter = Filter.or(
                Filter.and(
                        Filter.equalTo("role", "administrator"),
                        Filter.greaterThan("age", "30")
                ),
                Filter.equalTo("role", "superadmin")
        );

        assertTrue(filter.matches(Map.of("role", "administrator", "age", "35")));
        assertTrue(filter.matches(Map.of("role", "superadmin", "age", "20")));
        assertFalse(filter.matches(Map.of("role", "administrator", "age", "20")));
        assertFalse(filter.matches(Map.of("role", "user", "age", "40")));
    }

    @Test
    void deeplyNestedNotAndOr() {
        Filter filter = Filter.not(
                Filter.or(
                        Filter.equalTo("role", "administrator"),
                        Filter.and(
                                Filter.equalTo("department", "engineering"),
                                Filter.greaterThan("level", "5")
                        )
                )
        );

        assertTrue(filter.matches(Map.of("role", "user", "department", "sales", "level", "3")));
        assertFalse(filter.matches(Map.of("role", "administrator")));
        assertFalse(filter.matches(Map.of("role", "user", "department", "engineering", "level", "10")));
    }
}
