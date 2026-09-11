# Filter API — Design Reference

Working reference for this exercise, replacing `programming-exercise.pdf` as the doc we build from going forward. Contains the transcribed requirements, a breakdown of what's required vs. optional, and the design decisions made for the ambiguous parts of the spec.

## Source

Ping Identity developer programming exercise. Original PDF: `programming-exercise.pdf` (kept in repo for reference; this file supersedes it as the working doc).

## Background (as given)

An application manages access to "resource" objects (e.g. a user) and needs a Java API for querying the set of available resources, including filtering functionality so clients can select resources matching certain criteria. A resource is represented as `Map<String,String>` mapping property names (keys) to values. **Property names are case-sensitive; property values are not.**

## Task (as given)

Design and implement a `Filter` API that determines whether a resource matches a given set of criteria:

1. Ability to determine whether a filter matches a given resource (`Map<String,String>`).
2. Support for these filter predicate types (not all required — **at least one per category** a/b/c):
   - **(a) boolean literals**: `true`, `false`
   - **(b) logical operators** combining other filters: `AND`, `OR`, `NOT`
   - **(c) comparison operators** (must handle missing properties carefully):
     - i. property is present
     - ii. property equals some value
     - iii. property is less than some value
     - iv. property is greater than some value
     - v. property matches a regular expression
3. Ability to programmatically construct arbitrarily complex (nested) filters.
4. A string representation: generate a string from a filter, and (ideally) parse a filter from a string.
   **Explicitly not required**: the parsing direction — the PDF waives this to protect time.
5. Extensibility:
   - (a) how the API could be extended to support new filter types
   - (b) how the API could support 3rd-party code that needs to act on a filter's structure/content in a type-safe manner

### Worked example (as given)

```java
Map<String, String> user = new LinkedHashMap<String, String>();
user.put("firstname", "Joe");
user.put("surname", "Bloggs");
user.put("role", "administrator");
user.put("age", "35");

// Filter matching all administrators older than 30:
Filter filter = ???;
assert filter.matches(user);        // true

user.put("age", "25");
assert !filter.matches(user);       // now false
```

### Worked example, using the finalized API shape (see Decision 7)

```java
Filter filter = Filter.and(
    Filter.equalTo("role", "administrator"),
    Filter.greaterThan("age", "30")
);

filter.matches(user);       // resource passed per call, not bound to the filter
filter.matches(otherUser);  // same filter instance, reusable across resources
```

## Required vs. Optional

**Required:**
- `matches(resource)`-style evaluation entry point (item 1).
- At least one predicate from **each** of the three top-level categories — a boolean literal, a logical operator, a comparison operator (item 2). The literal floor is 3 predicate types total; the five comparison sub-types (i–v) are not individually mandatory.
- Programmatic, arbitrarily-nestable filter construction (item 3).
- String **generation** from a filter (item 4, generate direction only).
- Some treatment — implementation and/or written discussion — of extensibility (item 5).

**Explicitly optional:**
- Parsing filters from their string representation (item 4, parse direction) — PDF explicitly waives this.
- Implementing all 5 comparison sub-types or all 3 logical operators — breadth beyond the one-per-category floor is a bonus, not a requirement.

**Not mentioned by the spec at all (our own bar):**
- Unit tests. Given JUnit is part of the stack, expected for a professional submission but not a stated deliverable.

## Scope Decision

Implementing a focused subset rather than full breadth:
- **3 comparison types**: equals, less-than, greater-than. `is present` and regex-match are intentionally out of scope — the spec only requires one comparison sub-type (item 2c), and three already demonstrates meaningfully more than the floor while keeping the implementation surface smaller (one shared base class, no presence-only or regex-only code path to maintain).
- All 3 logical operators: AND, OR, NOT.
- Both boolean literals: true and false.
- Extensibility item 5b (type-safe 3rd-party structural access) addressed as a **written design discussion only** — no Visitor/sealed-type code implementation, to keep scope inside the time budget.

## Design Decisions

These resolve the ambiguities the spec left open. Each includes the decision and the reasoning/implication worth remembering.

### 1. Comparison value semantics — numeric-aware, applies to equals too
For **equals, less-than, and greater-than**, attempt to parse both the resource's property value and the filter's target value as **integers**. If both parse successfully, compare numerically. Otherwise, fall back to case-insensitive lexicographic string comparison. Decimal-looking values (e.g. `"5.0"`) are not treated as numeric — they fall back to string comparison like any other non-integer text, so `equalTo("price", "5.0")` does **not** match a resource value of `"5"`.

Applying this to `equals` (not just ordering) keeps behavior consistent — otherwise `"035" < "40"` would be true numerically while `"035" == "35"` stayed false under a strictly literal equals, which would look like a bug. See also Decision 6 (numeric normalization).

### 2. Missing-property semantics — 2-valued (true/false)
No three-valued "undefined" state. If a property is missing (or, per Decision 6, present-but-empty), every comparison predicate (equals, less-than, greater-than) simply evaluates to `false`.

**Known consequence, not a bug**: because this is 2-valued, `NOT(property equals X)` evaluates to `true` when the property is missing — a "not equal to X" filter matches resources that don't have the property at all, not just ones with a different value. This build's predicate set has no dedicated way to express "has the property AND it's not X" — `is present` was dropped from scope, and reintroducing it would be the way to recover that distinction. Worth a line in the write-up since it's a natural point of confusion.

### 3. String representation grammar — infix comparison operators, self-parenthesizing logical operators
Custom syntax. **Generation only** (`toString()`); no parser. Each filter type serializes itself and recursively delegates to its children.

Comparison/literal tokens render **bare, with no self-wrapping parens, ever** — a leaf predicate never parenthesizes itself, whether it's standalone or nested:

| Predicate | Syntax | Example |
|---|---|---|
| boolean literal | `true` / `false` | `true` |
| equals | `attr == value` | `role == 'administrator'` / `age == 30` |
| less than | `attr < value` | `age < 30` |
| greater than | `attr > value` | `age > 30` |

**Value quoting**: a comparison's target value is single-quoted unless it's numeric-looking, in which case it renders bare (`role == 'administrator'` vs `age == 30`). This reuses the same numeric-detection check from Decision 1's comparison semantics — same helper, applied to one value instead of two. Embedded quotes within a value are not escaped — acceptable since this direction is generation-only, with no parser to round-trip through.

Logical operators are **infix keywords**, and — unlike the leaf predicates — always self-parenthesize their own composed output, unconditionally, including at the root with no enclosing parent:

| Operator | Syntax | Example |
|---|---|---|
| AND (n-ary) | `(child1 AND child2 AND ... AND childN)` | `(role == 'administrator' AND age > 30)` |
| OR (n-ary) | `(child1 OR child2 OR ... OR childN)` | `(role == 'administrator' OR role == 'superadmin')` |
| NOT (unary) | `NOT (child)` | `NOT (role == 'administrator')` |

Generation rule for nesting: parenthesizing is a fixed, unconditional property of the filter's own type, not a decision made by whoever renders it — leaf predicates never wrap themselves.

- **AND/OR** synthesize their joined content by concatenating each child's `toString()` as-is (a leaf child contributes a bare fragment, a compound child arrives already self-wrapped), then wrap **that whole joined result** in exactly one pair of parens. A nested compound child is never re-wrapped by its parent — the single pair the parent adds surrounds the entire joined expression, not each child individually.
- **NOT** instead always wraps **its child specifically** — `"NOT (" + child.toString() + ")"` — unconditionally, regardless of whether the child already self-wraps. This is what makes the leaf case read correctly (`NOT (role == 'administrator')`, not `NOT role == 'administrator'`), but it means negating an already-self-wrapped compound child produces a harmless redundant double-paren: `NOT ((role == 'administrator' AND age > 30))`. This is accepted as-is — unambiguous, and keeping the rule unconditional avoids adding a child-type check purely to avoid a cosmetic redundancy (the same kind of conditional-by-child-type complexity intentionally avoided in AND/OR's design).

No parent-side branching by child type is needed anywhere — each logical operator's wrapping rule is fixed and unconditional, they just aren't the *same* rule. E.g.:

```java
Filter f = Filter.or(
    Filter.and(
        Filter.equalTo("role", "administrator"),
        Filter.greaterThan("age", "30")
    ),
    Filter.equalTo("role", "superadmin")
);
```
renders as:
`((role == 'administrator' AND age > 30) OR role == 'superadmin')`

Worked example (administrators older than 30):
`(role == 'administrator' AND age > 30)`

### 4. Scope target — 3 comparison types, not full breadth
See "Scope Decision" above: equals, less-than, greater-than (`is present` and regex-match are out of scope), all 3 logical operators, both boolean literals.

### 5. Extensibility 5b deliverable — written discussion only
No Visitor interface or sealed-type/pattern-matching code for type-safe 3rd-party structural access. Addressed as a design write-up (candidate approaches: classic Visitor, or Java 17 sealed interfaces + pattern-matching `switch` for compile-time exhaustiveness).

### 6. Value normalization edge cases
- **Numeric-looking values**: normalized per Decision 1 — `"35"` and `"035"` are considered the same value wherever numeric comparison applies (equals, less-than, greater-than).
- **Whitespace**: leading/trailing whitespace is trimmed from both the resource's property value and the filter's target value before any comparison.
- **Empty-string values and absent keys are equivalent everywhere**: a property whose value is empty after trimming is treated identically to a missing key for every comparison predicate.

### 7. Public API shape — static factories on `Filter` itself
The entire public surface is a single type: `Filter`. It carries both the evaluation contract (`matches(Map<String,String> resource)`, called per-resource — filters are reusable and not bound to a single resource) and the static factory methods used for programmatic construction (item 3): `Filter.and(...)`, `Filter.or(...)`, `Filter.not(...)`, `Filter.alwaysTrue()`, `Filter.alwaysFalse()`, `Filter.equalTo(...)`, `Filter.lessThan(...)`, `Filter.greaterThan(...)`. There is no separate factory class — mirrors JDK static-factory conventions like `Comparator`/`List`.

`matches` requires a non-null resource and throws `NullPointerException` otherwise — uniformly, for *every* filter type. Boolean literals and the logical operators don't strictly need the map, but letting them silently accept `null` while the comparison predicates threw would make the contract depend on which filter a caller happened to be holding.

Concrete predicate implementations (the classes actually created by these factories) stay package-private — a caller only ever holds a `Filter` reference. This is also the concrete answer to extensibility item 5a: a new predicate type is one new package-private class plus one new static factory method, with no existing class touched.

**Naming**: `equalTo`, not `equals` — a static `equals(String, String)` would compile without conflict alongside the inherited instance `Object.equals(Object)` (different signature), but reads confusingly next to it and is a known footgun some linters flag.

## Status
Requirements captured; all design decisions resolved, including the public API shape. Next step: implementation (see [IMPLEMENTATION.md](IMPLEMENTATION.md) for class/package structure and the test plan).
