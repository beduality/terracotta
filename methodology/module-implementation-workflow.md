---
description: Reusable workflow for implementing a production-ready Terracotta module after tests exist.
---

# Module Implementation Workflow

A repeatable workflow for turning a failing test suite into a production-ready
implementation in any `terracotta-*` module. It is designed to run after the TDD
workflow has established the required behavior, and focuses on building
maintainable, composable, well-documented code.

This is Phase 3 of the module development workflow. Start with
`module-development-workflow.md` if you have not read it.

## 1. Start from the failing tests

The implementation phase begins once there are tests describing the desired
behavior. If the tests are not yet written, follow the module TDD workflow first.

Run the tests to confirm the current failure mode:

```bash
./gradlew :<module>:test --tests "<fully.qualified.TestClass>"
```

Use the failure message as the guide for the first implementation step.

## 2. Satisfy the tests with the smallest change

Write the smallest amount of code that makes the new tests pass. Do not:

- Add unrelated features.
- Prematurely extract frameworks or abstractions.
- Optimize for performance without a measured reason.
- Leak implementation details into public APIs.

Keep implementation close to the tested behavior. It is acceptable for the first
pass to be simple or even naive; refactoring comes next.

## 3. Refactor toward production quality

Once tests pass, refactor the implementation. Verify the test suite stays green
after every change. Apply the following principles:

### Composability

- Build behavior from small, single-purpose units that can be combined.
- Prefer constructor injection and function parameters over global or mutable state.
- Favor immutable data structures and pure functions where side effects are not required.

### Modularity

- Keep each public class or function focused on one responsibility.
- Place internal helpers in `internal` packages or mark them `internal` so they do
  not become accidental public API.
- Avoid tight coupling to external libraries; introduce thin adapters at module
  boundaries.

### Extensibility

- Design for new variants through interfaces, sealed classes, or strategy objects
  rather than `if` chains over hard-coded values.
- Expose extension points (e.g., listeners, custom detectors, provider adapters) only
  when there is a concrete use case.
- Avoid requiring consumers to subclass framework types.

### Abstraction

- Abstract only after you have at least two concrete examples or a clear seam.
- Keep public APIs stable; hide unstable details behind `internal` or private members.
- Do not over-generalize one-off behavior into reusable machinery.

### Type safety

- Use Kotlin's type system to make invalid states unrepresentable.
- Prefer value classes, sealed classes, and non-nullable types over primitive `String`
  or `Boolean` flags when a concept has meaning.
- Avoid `Any` or unchecked casts in public APIs.

## 4. Stabilize the public API

The module's public API is what consumers see and what Dokka documents. Before
moving on:

- Review every public class, interface, object, enum, and top-level function.
- Remove accidental public members that were only needed internally.
- Keep parameter lists short; group related options into data classes when needed.
- Ensure binary compatibility is intentional. Mark experimental APIs with
  `@RequiresOptIn` if necessary.

## 5. Document with KDoc for Dokka

Every public API element must have useful KDoc so Dokka produces complete
reference documentation.

Add KDoc to:

- Public classes, interfaces, objects, and enums.
- Public constructors.
- Public functions and their parameters (`@param`) and return value (`@return`).
- Public properties, including data-class properties.
- Thrown exceptions (`@throws`).

Cross-link to user-facing guides where appropriate:

```kotlin
/**
 * Detects the release type from project metadata.
 *
 * @param metadata The local project metadata.
 * @return The detected release type, or `null` if no type could be inferred.
 * @see [Release types explanation](https://beduality.github.io/terracotta/content/core/explanation/release-types.html)
 */
```

Internal members should have enough KDoc to explain intent, but they do not need
Dokka-quality prose.

## 6. Add configuration and wiring

Update the module's `build.gradle.kts` if the implementation needs new
dependencies, source sets, or Gradle tasks.

Guidelines:

- Declare dependencies with the smallest scope needed (`implementation` preferred
  over `api`).
- Avoid exposing implementation dependencies as part of the public API.
- Use version catalogs (`libs.xxx`) for dependency versions.

## 7. Run the full verification suite

Verify the module builds, tests pass, and code quality checks succeed:

```bash
./gradlew :<module>:build :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply` to auto-fix formatting, then review the diff.

If the module publishes API docs:

```bash
./gradlew :<module>:dokkaHtml
```

Review the generated Dokka output to confirm public API documentation is
complete and links work.

## 8. Review before committing

Check the implementation against the project's standards:

- **Readable**: names reveal intent; complex logic is split into helper functions.
- **Tested**: every new behavior is covered by the TDD tests; edge cases are present.
- **Documented**: public API has KDoc; internal code is self-explanatory, with comments reserved for non-obvious intent or external constraints.
- **Consistent**: code follows the existing style enforced by Spotless.
- **Secure**: inputs are validated; secrets and tokens are not logged or exposed.

## Module placeholders

For shared placeholders, see `README.md` in this directory.

Phase-specific placeholders:

| Placeholder | Example value |
|-------------|---------------|
| Dokka output | `modules/<module>/build/dokka/html/` |

---

## Related files

- `README.md` — shared placeholders and commit/release guidance.
- `modules/<module>/src/main/kotlin/` — module source code.
- `modules/<module>/src/test/kotlin/` — tests that drive the implementation.
- `modules/<module>/build.gradle.kts` — module dependencies and build configuration.
- `gradle/libs.versions.toml` — shared dependency versions.
- `mkdocs.yml` — user-facing documentation navigation, if docs are part of the change.
