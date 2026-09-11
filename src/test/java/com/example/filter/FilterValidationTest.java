package com.example.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FilterValidationTest {

    @Test
    void rejectsNullConstructorArguments() {
        assertThrows(NullPointerException.class, () -> Filter.equalTo(null, "administrator"));
        assertThrows(NullPointerException.class, () -> Filter.equalTo("role", null));
        assertThrows(NullPointerException.class, () -> Filter.lessThan(null, "30"));
        assertThrows(NullPointerException.class, () -> Filter.greaterThan("age", null));
        assertThrows(NullPointerException.class, () -> Filter.not(null));
    }

    @Test
    void everyFilterTypeRejectsANullResource() {
        // The non-null contract is uniform across filter types — boolean literals and the logical
        // operators don't need the map, but accepting null there would make the contract depend on
        // which filter a caller happens to hold.
        assertThrows(NullPointerException.class, () -> Filter.alwaysTrue().matches(null));
        assertThrows(NullPointerException.class, () -> Filter.alwaysFalse().matches(null));
        assertThrows(NullPointerException.class, () -> Filter.equalTo("role", "admin").matches(null));
        assertThrows(NullPointerException.class, () -> Filter.lessThan("age", "30").matches(null));
        assertThrows(NullPointerException.class, () -> Filter.greaterThan("age", "30").matches(null));
        assertThrows(NullPointerException.class, () -> Filter.and(Filter.alwaysTrue()).matches(null));
        assertThrows(NullPointerException.class, () -> Filter.or(Filter.alwaysTrue()).matches(null));
        assertThrows(NullPointerException.class, () -> Filter.not(Filter.alwaysTrue()).matches(null));
    }
}
