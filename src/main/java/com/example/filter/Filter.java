package com.example.filter;

import java.util.List;
import java.util.Map;

/**
 * Determines whether a resource matches a set of criteria. A resource is represented as a
 * {@code Map<String,String>} of property names to values; property names are case-sensitive,
 * property values are not.
 *
 * <p>Filters are constructed via the static factory methods below and composed into arbitrarily
 * complex trees. A {@code Filter} instance is immutable and reusable across any number of
 * {@link #matches} calls against different resources.
 */
public interface Filter {

    boolean matches(Map<String, String> resource);

    @Override
    String toString();

    static Filter alwaysTrue() {
        return new BooleanLiteralFilter(true);
    }

    static Filter alwaysFalse() {
        return new BooleanLiteralFilter(false);
    }

    static Filter and(Filter... filters) {
        return new AndFilter(List.of(filters));
    }

    static Filter or(Filter... filters) {
        return new OrFilter(List.of(filters));
    }

    static Filter not(Filter filter) {
        return new NotFilter(filter);
    }

    static Filter equalTo(String property, String value) {
        return new EqualToFilter(property, value);
    }

    static Filter lessThan(String property, String value) {
        return new LessThanFilter(property, value);
    }

    static Filter greaterThan(String property, String value) {
        return new GreaterThanFilter(property, value);
    }
}
