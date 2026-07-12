---
description: Reusable TDD workflow for adding automated tests to a Terracotta module.
---

# Module Testing Workflow (TDD)

A repeatable test-first workflow for adding behavior to any `terracotta-*` module.
The goal is to write a failing test before writing the implementation, then iterate.

This is Phase 2 of the module development workflow. Start with
`module-development-workflow.md` if you have not read it.

## Test framework and location

Tests live in `modules/<module>/src/test/kotlin/<package>/` and run with JUnit 5
through Gradle's `useJUnitPlatform()`. Use `@Test`, `@TestFactory`, and standard
JUnit assertions. Configure extra source sets such as `functionalTest` or
`smokeTest` in `modules/<module>/build.gradle.kts` when needed.

## Test levels

| Level | Scope | Location | Use when |
|-------|-------|----------|----------|
| **Unit** | Single class or function; collaborators faked or not needed | `src/test/kotlin/` | Pure logic, algorithms, mapping, internal helpers |
| **Integration** | Multiple real classes working together; may hit parsers, repositories, or in-memory fakes | `src/test/kotlin/` | Data flow across layers, serialization, provider adapters |
| **Functional / E2E** | Module entry point from the outside, often through a CLI, task, or DSL | `src/test/kotlin/` or `src/functionalTest/kotlin/` | User-facing behavior, Gradle task execution, public API contracts |
| **Smoke** | A thin happy-path check that the module can start and run in a realistic environment | `src/smokeTest/kotlin/` or a standalone script | Release validation, CI health checks, deployment readiness |

Unit and integration tests live in `src/test/kotlin/`. Functional and smoke tests
use `src/functionalTest/kotlin/` and `src/smokeTest/kotlin/` once those source sets
are configured in `modules/<module>/build.gradle.kts`.

## 1. Identify the behavior to add or fix

Start from one of the following:

- A bug report or failing scenario.
- A new feature requirement.
- A public API contract that needs to be defined or hardened.

Write the behavior down as a concrete example. Avoid vague goals like
"make it work"; use specific inputs, outputs, and side effects.

## 2. Choose the right test level

Use the table in [Test levels](#test-levels) to pick the smallest level that gives
confidence for the change.

Avoid testing only through the largest surface when a smaller test can fail for
the same reason.

## 3. Write the failing test first

Create or open the matching test source set:

```
modules/<module>/src/test/kotlin/<package>/
```

Write a test that asserts the desired behavior, then run it and confirm it fails
for the expected reason:

```bash
./gradlew :<module>:test --tests "<fully.qualified.TestClass>"
```

A test that fails with a clear message is progress. A test that passes before
implementation is a warning sign: it may be asserting nothing or testing the
wrong thing.

## 4. Write the smallest implementation that makes the test pass

Implement just enough to satisfy the failing test. Do not add unrelated features,
optimizations, or abstractions yet.

Keep commits small: one test plus the matching implementation.

## 5. Refactor with the tests green

Once the test passes, clean up:

- Rename symbols for clarity.
- Remove duplication.
- Improve boundaries between classes.
- Add or tighten assertions if the test is too permissive.

Run the tests after each refactoring step to stay green.

## 6. Cover edge cases and error paths

Add tests for conditions that the happy path does not exercise:

- Empty collections, nulls, and invalid inputs.
- Failure branches and exceptions.
- Boundary values and concurrency where relevant.

Each new test should fail before the implementation handles it.

## 7. Run the module test suite and quality checks

Verify the full module test suite and formatting:

```bash
./gradlew :<module>:test :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply` to auto-fix formatting, then review the diff.

If the module has additional test source sets, run them too with
`./gradlew :<module>:functionalTest :<module>:smokeTest`. Fix failures before
moving on.

## 8. Review test quality

Before committing, check that each test earns its place:

- **Readable**: name and body explain the behavior.
- **Isolated**: no dependence on order or side effects of other tests.
- **Focused**: one concept per test; helper functions for shared setup.
- **Maintainable**: stable public APIs or well-defined internal seams.
