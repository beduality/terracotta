---
description: Write public interfaces and contracts with KDoc before tests and implementation.
---

# Module Contract Workflow

A focused workflow for turning an approved design into a stable, KDoc-covered public
interface (the contract) before tests are written and before the implementation
exists.

This phase encourages abstraction, clarifies seams for testing, and gives reviewers
a concrete API surface to evaluate early.

---

## Goal

Produce a stable public contract — interfaces, abstract types, SPI entries, and
data classes — that captures the design proposal without implementation details.

## Input from design phase

- Approved design proposal with public API sketch.
- Module boundaries and dependency rules.
- Edge cases and error conditions.

## Work

- Create or update the public API source files in `modules/<module>/src/main/kotlin/`.
- Prefer interfaces, abstract classes, and immutable data classes over concrete implementations.
- Add KDoc for every public symbol intended for Dokka.
- Mark extension points and seams explicitly.
- Avoid implementation details; leave them for Phase 3 (Implementation).
- Run `:<module>:compileKotlin` to verify the contract is valid and self-consistent.
- Update `CHANGELOG.md` or migration notes if the contract changes an existing public API.

## Output before proceeding

- Contract source files with KDoc in the correct public package.
- Passing `:<module>:compileKotlin`.
- A clear boundary between the contract and its planned implementations.

## Stop if

- The contract cannot be expressed without leaking implementation details. Return to Phase 1.
- The design proposal needs revision because the contract surface is too large or unstable.
