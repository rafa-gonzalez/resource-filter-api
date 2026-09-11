package com.example.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToStringTest {

    @Test
    void booleanLiterals() {
        assertEquals("true", Filter.alwaysTrue().toString());
        assertEquals("false", Filter.alwaysFalse().toString());
    }

    @Test
    void equalsQuotesNonNumericValuesButNotNumericOnes() {
        assertEquals("role == 'administrator'", Filter.equalTo("role", "administrator").toString());
        assertEquals("age == 30", Filter.equalTo("age", "30").toString());
    }

    @Test
    void lessThanAndGreaterThan() {
        assertEquals("age < 30", Filter.lessThan("age", "30").toString());
        assertEquals("age > 30", Filter.greaterThan("age", "30").toString());
    }

    @Test
    void andOrSelfParenthesizeIncludingAtTheRoot() {
        Filter and = Filter.and(Filter.equalTo("role", "administrator"), Filter.greaterThan("age", "30"));
        assertEquals("(role == 'administrator' AND age > 30)", and.toString());

        Filter or = Filter.or(Filter.equalTo("role", "administrator"), Filter.equalTo("role", "superadmin"));
        assertEquals("(role == 'administrator' OR role == 'superadmin')", or.toString());
    }

    @Test
    void notWrapsItsChildDirectly() {
        assertEquals("NOT (role == 'administrator')", Filter.not(Filter.equalTo("role", "administrator")).toString());
    }

    @Test
    void notOfACompoundChildProducesAcceptedRedundantDoubleParens() {
        // Documented in DESIGN.md Decision 3: NOT always wraps its child directly, regardless of
        // whether the child already self-wraps, so negating a compound AND/OR is intentionally
        // double-parenthesized rather than ambiguous or "fixed" with a child-type check.
        Filter filter = Filter.not(Filter.and(Filter.equalTo("role", "administrator"), Filter.greaterThan("age", "30")));
        assertEquals("NOT ((role == 'administrator' AND age > 30))", filter.toString());
    }

    @Test
    void deeplyNestedOrOfAndAndLeafMatchesTheDesignDocExample() {
        Filter filter = Filter.or(
                Filter.and(
                        Filter.equalTo("role", "administrator"),
                        Filter.greaterThan("age", "30")
                ),
                Filter.equalTo("role", "superadmin")
        );

        assertEquals("((role == 'administrator' AND age > 30) OR role == 'superadmin')", filter.toString());
    }
}
