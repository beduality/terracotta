---
description: Reusable TDD workflow for adding automated tests to a Terracotta module.
---

# Module Testing Workflow (TDD)

A repeatable test-first workflow for adding behavior to any `terracotta-*` module.
The goal is to write a failing test before writing the implementation, then iterate.

This is Phase 2 of the module development workflow. Start with
`module-development-workflow.md` if you have not read it.

## Test framework and location

Terracotta modules use **JUnit 5 (JUnit Jupiter)** through the Gradle `Test` task's
`useJUnitPlatform()` configuration. Tests live in:

```
modules/<module>/src/test/kotlin/<package>/
```

Write tests with `@Test` on regular methods, `@TestFactory` for dynamic / property-style
tests, and standard JUnit assertions (`assertEquals`, `assertTrue`, etc.).

If a module later adds extra source sets such as `functionalTest` or `smokeTest`,
configure them in `modules/<module>/build.gradle.kts` and run them as separate Gradle tasks.

## Test levels

| Level | Scope | Location | Use when |
|-------|-------|----------|----------|
| **Unit** | Single class or function; collaborators faked or not needed | `src/test/kotlin/` | Pure logic, algorithms, mapping, internal helpers |
| **Integration** | Multiple real classes working together; may hit parsers, repositories, or in-memory fakes | `src/test/kotlin/` | Data flow across layers, serialization, provider adapters |
| **Functional / E2E** | Module entry point from the outside, often through a CLI, task, or DSL | `src/test/kotlin/` or `src/functionalTest/kotlin/` | User-facing behavior, Gradle task execution, public API contracts |
| **Smoke** | A thin happy-path check that the module can start and run in a realistic environment | `src/smokeTest/kotlin/` or a standalone script | Release validation, CI health checks, deployment readiness |

In this project today, unit and integration tests live in `src/test/kotlin/`. Functional and
smoke tests are not yet separated into their own source sets, but this workflow reserves those
names for when a module needs them. Adding `functionalTest` or `smokeTest` requires configuring
the source set and task in `modules/<module>/build.gradle.kts` first.

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

For functional / integration tests that need resources, prefer:

```
modules/<module>/src/functionalTest/kotlin/<package>/
```

Write a test that asserts the desired behavior. Run it and confirm it fails for
the expected reason:

```bash
./gradlew :<module>:test --tests "<fully.qualified.TestClass>"
```

A test that fails with a clear message is progress. A test that passes before
implementation is a warning sign: it may be asserting nothing or testing the
wrong thing.

## 4. Write the smallest implementation that makes the test pass

Implement just enough to satisfy the failing test. Do not add unrelated features,
optimizations, or abstractions yet.

Use the test feedback loop:

```bash
./gradlew :<module>:test --tests "<fully.qualified.TestClass>"
```

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

If the module has additional test source sets, run them too:

```bash
./gradlew :<module>:functionalTest :<module>:smokeTest
```

Fix failures before considering the work done.

## 8. Review test quality

Before committing, check that each test earns its place:

- **Readable**: the test name and body explain the behavior.
- **Isolated**: it does not depend on the order or side effects of other tests.
- **Focused**: it asserts one concept per test; use helper functions for shared setup.
- **Maintainable**: it uses stable public APIs or well-defined internal seams.

## Module placeholders

For shared placeholders, see `README.md` in this directory.

Phase-specific placeholders:

| Placeholder | Example value |
|-------------|---------------|
| Test framework | JUnit 5 (JUnit Jupiter) |

---

## Related files

- `README.md` — shared placeholders and commit/release guidance.
- `modules/<module>/src/main/kotlin/` — module source code.
- `modules/<module>/src/test/kotlin/` — unit and integration tests.
- `modules/<module>/src/functionalTest/kotlin/` — functional tests, if configured.
- `modules/<module>/src/smokeTest/kotlin/` — smoke tests, if configured.
- `modules/<module>/build.gradle.kts` — test source sets, dependencies, and plugins.
