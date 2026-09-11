# resource-filter-api

A Java API for filtering resource objects (`Map<String,String>`) based on composable criteria.

A *resource* is a map of property names to values — for example a user with `role`, `age`, and
`department`. A *filter* decides whether a given resource matches some criteria, and filters compose
into arbitrarily nested trees.

## Requirements

Java 17, Maven.

## Build and test

```bash
mvn test
```

## Quick start

```java
Map<String, String> user = new LinkedHashMap<>();
user.put("firstname", "Joe");
user.put("surname",   "Bloggs");
user.put("role",      "administrator");
user.put("age",       "35");

// Matches all administrators older than 30:
Filter filter = Filter.and(
    Filter.equalTo("role", "administrator"),
    Filter.greaterThan("age", "30")
);

filter.matches(user);        // true
System.out.println(filter);  // (role == 'administrator' AND age > 30)

user.put("age", "25");
filter.matches(user);        // false — same filter instance, re-evaluated
```

Filters are immutable, thread-safe, and unbound to any resource: the resource is passed per
`matches` call, so one filter can be evaluated against any number of resources.

## Supported filters

| Category | Factory | Renders as |
|---|---|---|
| Boolean literal | `Filter.alwaysTrue()` / `Filter.alwaysFalse()` | `true` / `false` |
| Logical | `Filter.and(...)` (n-ary) | `(a AND b)` |
| Logical | `Filter.or(...)` (n-ary) | `(a OR b)` |
| Logical | `Filter.not(f)` | `NOT (a)` |
| Comparison | `Filter.equalTo(property, value)` | `role == 'administrator'` |
| Comparison | `Filter.lessThan(property, value)` | `age < 30` |
| Comparison | `Filter.greaterThan(property, value)` | `age > 30` |

`and` and `or` require at least one filter; calling them with none throws
`IllegalArgumentException`. Every filter rejects a null resource with `NullPointerException`.

## String representation

`toString()` generates a filter's string form. Parsing the format back into a filter is **not**
implemented — the exercise explicitly waives that direction.

Comparison and literal predicates render bare, never parenthesizing themselves. A comparison's value
is single-quoted unless it looks like an integer, in which case it renders bare (`role ==
'administrator'` vs. `age == 30`).

Logical operators are infix keywords that always wrap their own output in exactly one pair of
parentheses — including at the top level:

```java
Filter.or(
    Filter.and(
        Filter.equalTo("role", "administrator"),
        Filter.greaterThan("age", "30")
    ),
    Filter.equalTo("role", "superadmin")
);
// ((role == 'administrator' AND age > 30) OR role == 'superadmin')
```

`NOT` is the one asymmetric case: it always wraps its child directly, whether or not that child
already wrapped itself. Negating a compound filter therefore yields a redundant-looking but
unambiguous double paren — `NOT ((a AND b))` — which is accepted deliberately rather than fixed with
a child-type check.

## Matching semantics

- **Property names are case-sensitive; property values are not.** A filter on `Role` will not read a
  resource's `role` entry, but `administrator` matches `ADMINISTRATOR`.
- **A missing property never matches**, and a value that is empty after trimming is treated exactly
  like a missing one.
- **Values are trimmed** on both sides before comparison.
- **Integers compare numerically**, so `"035"` equals `"35"` and `"9"` is less than `"10"`. If either
  side isn't an integer, both compare as case-insensitive text — which means decimals like `"5.0"`
  compare as text, not numbers.
- **Matching is two-valued** (true/false), with no "undefined" state. A consequence worth knowing:
  `not(equalTo("role", "admin"))` matches resources that lack `role` entirely, not just those with a
  different role.

## Extending the API

Adding a new predicate touches nothing that already exists: write one new package-private class
implementing `Filter` (or extending `AbstractPropertyFilter` if it compares a property against a
value), then add one static factory method to `Filter`.

Every concrete filter class is package-private and callers only ever hold a `Filter` reference, so
new predicates, and changes to existing ones, stay invisible to calling code. The three comparison
predicates demonstrate the pattern: `AbstractPropertyFilter` owns the property lookup, trimming,
missing/empty handling, numeric-vs-text comparison and string rendering, leaving each subclass just
two lines — which sign of the comparison counts as a match, and which operator token to render.

## Design decisions

[DESIGN.md](DESIGN.md) records the requirements and every decision made where the spec was
ambiguous — comparison semantics, missing-property handling, the string grammar, and scope.
[IMPLEMENTATION.md](IMPLEMENTATION.md) covers class structure and the test plan.

## Known tradeoffs

- **Scope**: `is present` and regex-match predicates are deliberately out of scope. The spec requires
  at least one comparison operator; three demonstrate the pattern without extra surface area.
- **Integer parsing uses exceptions for control flow** — `Integer.valueOf` in a try/catch on each
  comparison. A digit pre-check would avoid the exception cost, but it duplicates parsing rules for a
  gain this workload doesn't need.
- **No `equals`/`hashCode` on filters.** They'd only pay off if callers compared or de-duplicated
  filters, which nothing here requires; adding them across every class would be noise.
