package com.example.filter;

import java.util.List;
import java.util.Map;

/**
 * Determines whether a resource matches a set of criteria. A resource is represented as a
 * {@code Map<String,String>} of property names to values; property names are case-sensitive,
 * property values are not.
 *
 * <p>Filters are constructed via the static factory methods below and composed into arbitrarily
 * complex trees. A {@code Filter} instance is immutable, thread-safe, and reusable across any
 * number of {@link #matches} calls against different resources.
 *
 * <p>Comparison semantics shared by {@link #equalTo}, {@link #lessThan} and {@link #greaterThan}:
 * <ul>
 *   <li>A property that is absent, or whose value is empty after trimming, never matches.
 *   <li>Both values are trimmed before comparison.
 *   <li>If both values parse as integers they compare numerically, so {@code "035"} equals
 *       {@code "35"}. Otherwise they compare case-insensitively as text — decimals such as
 *       {@code "5.0"} are not integers and therefore compare as text.
 * </ul>
 */
public interface Filter {

    /**
     * @param resource property names to values; must not be null
     * @return whether this filter matches the given resource
     * @throws NullPointerException if {@code resource} is null
     */
    boolean matches(Map<String, String> resource);

    /**
     * @return this filter rendered in the string representation described in {@code DESIGN.md},
     *         e.g. {@code (role == 'administrator' AND age > 30)}
     */
    @Override
    String toString();

    /** @return a filter matching every resource. */
    static Filter alwaysTrue() {
        return new BooleanLiteralFilter(true);
    }

    /** @return a filter matching no resource. */
    static Filter alwaysFalse() {
        return new BooleanLiteralFilter(false);
    }

    /**
     * @param filters one or more filters to combine; all must match
     * @throws IllegalArgumentException if no filters are given
     */
    static Filter and(Filter... filters) {
        return new AndFilter(List.of(filters));
    }

    /**
     * @param filters one or more filters to combine; at least one must match
     * @throws IllegalArgumentException if no filters are given
     */
    static Filter or(Filter... filters) {
        return new OrFilter(List.of(filters));
    }

    /**
     * Negates a filter. Note that because matching is two-valued, {@code not(equalTo(p, v))}
     * also matches resources lacking property {@code p} entirely.
     */
    static Filter not(Filter filter) {
        return new NotFilter(filter);
    }

    /** @return a filter matching resources whose {@code property} equals {@code value}. */
    static Filter equalTo(String property, String value) {
        return new EqualToFilter(property, value);
    }

    /** @return a filter matching resources whose {@code property} is less than {@code value}. */
    static Filter lessThan(String property, String value) {
        return new LessThanFilter(property, value);
    }

    /** @return a filter matching resources whose {@code property} is greater than {@code value}. */
    static Filter greaterThan(String property, String value) {
        return new GreaterThanFilter(property, value);
    }
}
