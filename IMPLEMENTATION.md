# Filter API — Implementation Plan

Architecture-level plan built on the decisions in [DESIGN.md](DESIGN.md). This file covers class/package structure and the test plan; DESIGN.md stays focused on requirements and the resolved ambiguities.

## Package layout

Everything lives in a **single package** — package-private visibility doesn't cross sub-packages, and keeping the implementation classes package-private is how extensibility item 5a is answered concretely: adding a new predicate later means one new package-private class + one new static factory method on `Filter`, with zero existing classes touched and no caller-visible change (callers only ever hold a `Filter` reference).

The public API surface is a single type: `Filter` itself carries the static factory methods (`Filter.and(...)`, `Filter.equalTo(...)`, etc.) — no separate factory class. This mirrors `Comparator`/`List`-style static factories in the JDK.

```
com.example.filter/
  Filter.java                       public interface + static factories — the only type callers hold
  AbstractPropertyFilter.java       package-private abstract base for the 3 comparison predicates —
                                     holds property/value state, the numeric-parse attempt, the
                                     missing/empty-as-not-matching template, and toString() quoting
  EqualToFilter.java                package-private
  LessThanFilter.java               package-private
  GreaterThanFilter.java            package-private
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
    static Filter equalTo(String property, String value) { ... }
    static Filter lessThan(String property, String value) { ... }
    static Filter greaterThan(String property, String value) { ... }
}
```

String generation (DESIGN.md Decision 3) needs no per-type branching by whoever renders a child — parenthesizing is a fixed property of each filter's own type: leaf predicates (comparisons, literals) render **bare**, never wrapping themselves. `AndFilter`/`OrFilter` join children's `toString()` as-is (bare for a leaf, already-self-wrapped for a compound child) and wrap the whole joined result in one pair of parens — a nested compound child is never re-wrapped. `NotFilter` instead always wraps its single child specifically (`"NOT (" + child + ")"`), unconditionally — this is a *different* fixed rule than AND/OR's, not the same rule applied uniformly, and it means negating an already-self-wrapped compound child produces a harmless, accepted redundant double-paren (e.g. `NOT ((a AND b))`). See DESIGN.md Decision 3 for the full reasoning.

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

### AbstractPropertyFilter — shared state, missing/empty handling, numeric comparison (Decisions 1, 2 & 6)

Holds `property`, `rawValue` (trimmed target value), and `numericValue` (the target value pre-parsed as an `Integer`, or `null` if it isn't one) — computed once in the constructor. `matches()` is `final` on the base class, so every comparison predicate inherits the missing/empty-as-not-matching rule and the numeric-vs-lexicographic fallback for free instead of each repeating it:

```java
public final boolean matches(Map<String, String> resource) {
    String resourceValue = resource.get(property);
    if (resourceValue == null) return false;

    String trimmed = resourceValue.trim();
    if (trimmed.isEmpty()) return false;

    int comparisonResult = (numericValue != null && tryParseInt(trimmed) != null)
            ? Integer.compare(tryParseInt(trimmed), numericValue)
            : trimmed.compareToIgnoreCase(rawValue);

    return matchesComparison(comparisonResult);
}
protected abstract boolean matchesComparison(int comparisonResult);
```

`comparisonResult` is the sign of (resource value) compared to (target value) — numeric if both parse as integers, else case-insensitive lexicographic (`compareToIgnoreCase(...) == 0` is equivalent to `equalsIgnoreCase`, which is what makes this one code path work for equals as well as the ordering comparisons). Each subclass reduces to a one-line sign check: `EqualToFilter` → `comparisonResult == 0`, `LessThanFilter` → `< 0`, `GreaterThanFilter` → `> 0`.

### Comparison predicates — `toString()` rendering (Decision 3)

Also templated on the base class: `attr OPERATOR value`, where `value` is single-quoted unless `numericValue != null` (e.g. `role == 'administrator'` vs `age == 30`). Each subclass supplies only its operator token via `operator()`: `"=="`, `"<"`, `">"`.

### Logical operators — AndFilter / OrFilter / NotFilter

- **Construction**: `AndFilter`/`OrFilter` hold an unmodifiable, defensively-copied `List<Filter>`, validated **non-empty** — constructor throws `IllegalArgumentException` on zero children (see Validation/assumptions below). `NotFilter` holds a single non-null `Filter` child.
- **`matches()`**: `AndFilter` → `allMatch`, `OrFilter` → `anyMatch`, `NotFilter` → negation.
- **`toString()`**: `AndFilter`/`OrFilter` join children's `toString()` directly (no per-child wrapping) with `" AND "`/`" OR "` and wrap the whole joined result in one pair of parens; `NotFilter` renders `"NOT (" + child.toString() + ")"`, always wrapping its child directly (a different, not identical, fixed rule — see the redundant-double-paren note above). Neither rule branches on the child's actual type; they just aren't the same rule.

### BooleanLiteralFilter

Holds a constant; `toString()` returns `"true"`/`"false"`, bare — like every other leaf predicate, it never wraps itself in parens.

## Validation/assumptions
- **Empty AND/OR is illegal**: `Filter.and()`/`Filter.or()` called with zero arguments throws `IllegalArgumentException`, rather than adopting a vacuous-truth convention (empty AND = true, empty OR = false). DESIGN.md's task notes had flagged this as unresolved; settled in favor of the simpler, less-surprising option.
- `NotFilter` and all property filters reject `null` arguments via `Objects.requireNonNull`.
- Property name lookup is a literal `Map.get()` — names are case-sensitive per spec, no normalization applied to keys (only to values, per Decision 6).

## Build
- Maven, Java 17 (`maven.compiler.release=17`).
- `junit-jupiter` (test scope), `maven-surefire-plugin`.
- `src/main/java/com/example/filter/*.java`, `src/test/java/com/example/filter/*.java`.
- groupId/package: `com.example.filter`.

## Test plan (JUnit 5)
- One test class per predicate type, each covering: match / non-match / missing-property / empty-string-property / case-insensitivity / numeric normalization (where applicable) / whitespace trimming (where applicable).
- `CompositionTest` — nested arbitrary-depth filters, including the spec's worked example (administrators older than 30) verbatim.
- `ToStringTest` — exact-string assertions against the Decision 3 grammar: each predicate's bare rendering, value quoting (numeric-bare vs. string-quoted), the self-parenthesizing nested case (the `OR(AND(...), ...)` example from DESIGN.md), and `NOT`'s accepted redundant double-paren when negating a compound child (`NOT ((a AND b))`) — asserted explicitly so it reads as intended, not a future "bug fix" regression.
- An explicit test asserting the `NOT(equals)`-matches-on-missing-property behavior called out in DESIGN.md Decision 2, so it reads as intended behavior rather than a future "bug fix" regression.

## Deferred (per Decision 5)
5b (type-safe 3rd-party structural access) gets no code — written discussion only, to be drafted once the core implementation is in place. Candidates to weigh there: classic Visitor pattern vs. Java 17 sealed interfaces + pattern-matching `switch` for compile-time exhaustiveness.

## Status
Mid-rebuild: `Filter`, `AbstractPropertyFilter`, `EqualToFilter`, `LessThanFilter`, `GreaterThanFilter`, `AndFilter` implemented and passing their tests. `OrFilter.java` exists but isn't wired to `Filter.or()` yet, and its `matches()` currently implements AND semantics (copied from `AndFilter`, not yet adapted to `anyMatch`) — needs a look before wiring it up. `NotFilter`, `BooleanLiteralFilter`, and the `alwaysTrue`/`alwaysFalse`/`not` factory wiring are still outstanding. `is present` and regex-match are out of scope entirely (Decision 4) — no `PresentFilter`/`RegexFilter` to build.
