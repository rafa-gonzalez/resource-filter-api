# Working with Claude Code on This Exercise

This exercise was built with Claude Code as a collaborator throughout — for requirements analysis,
design discussion, code review, and documentation. This note describes the working process, since
the process is part of what's being evaluated as much as the code itself.

## 1. Understanding before design

Before any design or code, the first request was a pure comprehension pass: summarize what the
spec actually requires vs. what's optional, flag every place it's ambiguous, and ask clarifying
questions — explicitly *not* to propose an architecture yet. Nothing was designed against an
assumption; every ambiguity the spec left open (comparison semantics, missing-property handling,
scope, the string grammar) was surfaced and resolved as an explicit decision before it was built,
not discovered as a bug later.

## 2. Specifying with concrete examples, not descriptions

Where a description would have been ambiguous, I gave a worked example instead. The clearest
instance: rather than describing the desired string grammar in prose, I supplied an exact
`Filter.or(Filter.and(...), ...)` call alongside its exact expected `toString()` output. That one
example pinned the entire self-parenthesizing rule precisely — including the asymmetry between how
`AND`/`OR` and `NOT` wrap their children — far more precisely than a written spec would have.

## 3. Recording decisions in living documents, not chat history

Once ambiguities were resolved, the requirements and every design decision were consolidated into
`DESIGN.md`, explicitly superseding the original PDF as the working reference. This meant the
reasoning behind each decision (not just the decision itself) persisted across the session and
stayed available to re-check design choices against later — including correcting the docs when
they drifted from what was actually built (see #8), and later merging a separate
`IMPLEMENTATION.md` back into `DESIGN.md` once the project was small enough that splitting
decision-rationale from architecture wasn't earning its keep.

## 4. Treating AI proposals as a draft, not the answer

Generated designs were reviewed critically, not accepted by default. When the AI's first
string-generation design used a polymorphic `toNestedString()` override mechanism, I rejected it as
unnecessary cleverness and asked for a simpler, unconditional rule instead. Separately, I asked the
AI to audit its own prior design for internal inconsistencies, which surfaced a real asymmetry (`NOT`
double-parenthesizing a compound child differently than `AND`/`OR` handle nested children) that
neither of us had caught in the moment — worth documenting as accepted behavior rather than silently
patching.

## 5. Owning the implementation; using AI as reviewer, not author

After seeing a fully AI-generated implementation, I deliberately reverted it — keeping only the
Maven scaffold and interface stubs — and hand-wrote every class myself, using the AI purely as a
line-by-line reviewer at each step. That review loop caught real, specific bugs as they were
written: a `private` modifier that would have failed to compile, a missing return statement, AND
semantics accidentally copy-pasted into an `OR` filter, an `Integer`-only parser silently rejecting
valid decimal input, and inconsistent null-checks across sibling classes. Getting this feedback
per-class, immediately, was more useful than a single end-of-task review would have been.

## 6. Verifying continuously, not trusting either side's claims

The test suite was run after essentially every change — including changes proposed by the AI —
rather than trusting a description of what the code should do. When new test cases were added
during a later review pass, they were run against the existing implementation *unchanged* first, to
confirm whether they exposed a real bug or just added coverage. (They added coverage — a useful
signal in itself.)

## 7. Curating suggestions instead of accepting them wholesale

When asked for a broad improvement pass (naming, comments, refactors, complexity, test gaps), I
accepted some suggestions and explicitly declined others with reasons — for instance, rejecting an
`AbstractLogicalFilter` extraction because it would abstract over only two call sites, and rejecting
a stylistic stream-based rewrite of a working loop. The goal was matching the codebase's complexity
to its actual size, not applying every available refactor.

## 8. Controlling scope deliberately, and keeping the docs honest

Scope was narrowed twice, deliberately: dropping the `is present` and regex predicates after the
required floor was already exceeded by the three comparison operators implemented, and later
narrowing numeric comparison from "integer or decimal" to integers only, once the hand-written
implementation revealed that was the simpler and sufficient behavior. In both cases the design docs
were updated to match — including once explicitly reversing a documentation change I'd asked for,
to make `DESIGN.md` match the implementation instead of asking the implementation to grow to match
an aspirational document.

## Outcome

A fully documented design record (`DESIGN.md`), a hand-written implementation
reviewed at every step, and a test suite that grew alongside it rather than being written
after the fact — 48 tests, all passing, including cases added specifically to pin down previously
untested branches.
