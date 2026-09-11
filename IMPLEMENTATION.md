# Filter API — Implementation Plan

Architecture-level plan built on the decisions in [DESIGN.md](DESIGN.md). This file covers class/package structure and the test plan; DESIGN.md stays focused on requirements and the resolved ambiguities.

## Package layout

Everything lives in a **single package** — package-private visibility doesn't cross sub-packages, and keeping the implementation classes package-private is how extensibility item 5a is answered concretely: adding a new predicate later means one new package-private class + one new static factory method on `Filter`, with zero existing classes touched and no caller-visible change (callers only ever hold a `Filter` reference).

The public API surface is a single type: `Filter` itself carries the static factory methods (`Filter.and(...)`, `Filter.equalTo(...)`, etc.) — no separate factory class. This mirrors `Comparator`/`List`-style static factories in the JDK.

```
com.example.filter/
  Filter.java                       public interface + static factories — the only type callers hold
  Values.java                       package-private: normalize() (trim, empty-as-null), compare() (numeric-aware),
                                     isNumeric() (shared by compare() and toString() value-quoting)
  AbstractPropertyFilter.java       package-private abstract base for the 5 comparison predicates
  PresentFilter.java                package-private
  EqualsFilter.java                 package-private
  LessThanFilter.java               package-private
  GreaterThanFilter.java            package-private
  RegexFilter.java                  package-private
  AndFilter.java                    package-private
  OrFilter.java                     package-private
  NotFilter.java                    package-private
  BooleanLiteralFilter.java         package-private
```

## Core interface

```java
public interface Filter {
    boolean matches(Map<String, String> resource);

    @Override
    String toString(); // redeclared for documentation; Java can't enforce this via interface

    // Static factories — the entire public entry point for constructing filters.
    static Filter alwaysTrue() { ... }
    static Filter alwaysFalse() { ... }
    static Filter and(Filter... filters) { ... }
    static Filter or(Filter... filters) { ... }
    static Filter not(Filter filter) { ... }
    static Filter present(String property) { ... }
    static Filter equalTo(String property, String value) { ... }
    static Filter lessThan(String property, String value) { ... }
    static Filter greaterThan(String property, String value) { ... }
    static Filter matchesRegex(String property, String pattern) { ... }
}
```

String generation (DESIGN.md Decision 5) needs no per-type branching by whoever renders a child — parenthesizing is a fixed property of each filter's own type: leaf predicates (comparisons, literals) render **bare**, never wrapping themselves. `AndFilter`/`OrFilter` join children's `toString()` as-is (bare for a leaf, already-self-wrapped for a compound child) and wrap the whole joined result in one pair of parens — a nested compound child is never re-wrapped. `NotFilter` instead always wraps its single child specifically (`"NOT (" + child + ")"`), unconditionally — this is a *different* fixed rule than AND/OR's, not the same rule applied uniformly, and it means negating an already-self-wrapped compound child produces a harmless, accepted redundant double-paren (e.g. `NOT ((a AND b))`). See DESIGN.md Decision 5 for the full reasoning.

**Naming note**: `equalTo` (not `equals`) — a static `equals(String, String)` would compile fine alongside the inherited instance `Object.equals(Object)` (different signature, no real conflict), but reads oddly sitting next to it and is a known gotcha some linters flag. Confirmed with the exercise owner; going with `equalTo`.

## Usage shape

```java
Filter filter = Filter.and(
    Filter.equalTo("role", "administrator"),
    Filter.greaterThan("age", "30")
);

filter.matches(user);       // resource passed per call, not bound to the filter
filter.matches(otherUser);  // same filter, reusable across resources

System.out.println(filter); // (role == 'administrator' AND age > 30)
```

## Shared mechanics

Grouped by which class(es) each mechanism lives in — property-based comparison filters share a base class and a helper, logical operators and boolean literals are their own thing.

### AbstractPropertyFilter — missing/empty-value handling (Decisions 2 & 8)

`matches()` is `final` on the base class, so every comparison predicate inherits the missing/empty-as-not-matching rule for free instead of repeating the check five times:

```java
public final boolean matches(Map<String, String> resource) {
    String normalized = Values.normalize(resource.get(propertyName));
    if (normalized == null) return false;
    return matchesValue(normalized);
}
protected abstract boolean matchesValue(String normalizedValue);
```

`PresentFilter.matchesValue()` just returns `true` — reaching that point already proves the property is present and non-empty.

### Values — numeric-aware comparison and quoting (Decision 1)

`Values.compare(String a, String b)`: attempt to parse both as numbers, compare numerically if both succeed, else fall back to case-insensitive lexicographic comparison. `EqualsFilter` checks `== 0`; `LessThanFilter`/`GreaterThanFilter` check the sign. Same helper, three call sites — keeps the numeric-normalization behavior consistent by construction rather than by convention.

`Values.isNumeric(String)` is factored out of `compare()` and reused by the `toString()` value-quoting rule below, so "is this numeric" is answered in exactly one place.

### Comparison predicates — `toString()` rendering (Decision 5)

`EqualsFilter`/`LessThanFilter`/`GreaterThanFilter` render `attr OP value`, where `value` is single-quoted unless `Values.isNumeric(value)` is true (e.g. `role == 'administrator'` vs `age == 30`). `PresentFilter` renders `attr IS PRESENT` (no value). `RegexFilter` renders `attr MATCHES 'pattern'` — always quoted, since a pattern is never numeric.

### RegexFilter — match semantics (Decisions 3 & 4)

Compiles with `Pattern.CASE_INSENSITIVE`; `matchesValue()` uses `pattern.matcher(normalizedValue).find()` (partial match, not `String.matches()`).

### Logical operators — AndFilter / OrFilter / NotFilter

- **Construction**: `AndFilter`/`OrFilter` hold an unmodifiable, defensively-copied `List<Filter>`, validated **non-empty** — constructor throws `IllegalArgumentException` on zero children (see Validation/assumptions below). `NotFilter` holds a single non-null `Filter` child.
- **`matches()`**: `AndFilter` → `allMatch`, `OrFilter` → `anyMatch`, `NotFilter` → negation.
- **`toString()`**: `AndFilter`/`OrFilter` join children's `toString()` directly (no per-child wrapping) with `" AND "`/`" OR "` and wrap the whole joined result in one pair of parens; `NotFilter` renders `"NOT (" + child.toString() + ")"`, always wrapping its child directly (a different, not identical, fixed rule — see the redundant-double-paren note above). Neither rule branches on the child's actual type; they just aren't the same rule.

### BooleanLiteralFilter

Holds a constant; `toString()` returns `"true"`/`"false"`, bare — like every other leaf predicate, it never wraps itself in parens.

## Validation/assumptions
- **Empty AND/OR is illegal**: `Filter.and()`/`Filter.or()` called with zero arguments throws `IllegalArgumentException`, rather than adopting a vacuous-truth convention (empty AND = true, empty OR = false). DESIGN.md's task notes had flagged this as unresolved; settled in favor of the simpler, less-surprising option.
- `NotFilter` and all property filters reject `null` arguments via `Objects.requireNonNull`.
- Property name lookup is a literal `Map.get()` — names are case-sensitive per spec, no normalization applied to keys (only to values, per Decision 8).

## Build
- Maven, Java 17 (`maven.compiler.release=17`).
- `junit-jupiter` (test scope), `maven-surefire-plugin`.
- `src/main/java/com/example/filter/*.java`, `src/test/java/com/example/filter/*.java`.
- groupId/package: `com.example.filter`.

## Test plan (JUnit 5)
- One test class per predicate type, each covering: match / non-match / missing-property / empty-string-property / case-insensitivity / numeric normalization (where applicable) / whitespace trimming (where applicable).
- `CompositionTest` — nested arbitrary-depth filters, including the spec's worked example (administrators older than 30) verbatim.
- `ToStringTest` — exact-string assertions against the Decision 5 grammar: each predicate's bare rendering, value quoting (numeric-bare vs. string-quoted), the self-parenthesizing nested case (the `OR(AND(...), ...)` example from DESIGN.md), and `NOT`'s accepted redundant double-paren when negating a compound child (`NOT ((a AND b))`) — asserted explicitly so it reads as intended, not a future "bug fix" regression.
- An explicit test asserting the `NOT(equals)`-matches-on-missing-property behavior called out in DESIGN.md Decision 2, so it reads as intended behavior rather than a future "bug fix" regression.

## Deferred (per Decision 7)
5b (type-safe 3rd-party structural access) gets no code — written discussion only, to be drafted once the core implementation is in place. Candidates to weigh there: classic Visitor pattern vs. Java 17 sealed interfaces + pattern-matching `switch` for compile-time exhaustiveness.

## Status
Architecture planned, not yet implemented. Next step: scaffold the Maven project and implement per this plan.
