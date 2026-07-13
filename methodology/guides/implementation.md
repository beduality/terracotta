---
description: Reusable workflow for implementing a production-ready Terracotta module after tests exist.
---

# Module Implementation Workflow

A repeatable workflow for turning a failing test suite into a production-ready
implementation in any `terracotta-*` module. Runs after the TDD workflow and
focuses on maintainable, composable, well-documented code.

This is Phase 5 of the module development workflow. Start with
`development.md` if you have not read it.

## 1. Start from the failing tests

Begin once tests describe the desired behavior. If tests are not written, follow
the testing workflow first.

Run the tests to confirm the current failure:

```bash
./gradlew :<module>:test --tests "<fully.qualified.TestClass>"
```

## 2. Satisfy the tests with the smallest change

Write the smallest amount of code that makes the new tests pass. Avoid unrelated
features, premature abstractions, unmeasured optimizations, and leaking
implementation details into public APIs. The first pass can be simple; refactoring
comes next.

## 3. Refactor toward production quality

Once tests pass, refactor. Verify the suite stays green after every change.

- **Composability**: build behavior from small, single-purpose units; prefer
  constructor injection, immutable data, and pure functions.
- **Modularity**: one responsibility per public class or function; mark helpers
  `internal`; use thin adapters at module boundaries.
- **Extensibility**: favor interfaces, sealed classes, and strategy objects over
  hard-coded `if` chains; expose extension points only for concrete use cases.
- **Abstraction**: abstract only after two concrete examples or a clear seam;
  keep public APIs stable and hide unstable details.
- **Type safety**: use value classes, sealed classes, and non-null types; avoid
  `Any` and unchecked casts in public APIs.

## 4. Stabilize the public API

The public API is what consumers see and Dokka documents. Before moving on:

- Review every public class, interface, object, enum, and top-level function.
- Remove accidental public members.
- Keep parameter lists short; group related options into data classes.
- Mark experimental APIs with `@RequiresOptIn` if necessary.

## 5. Document with KDoc for Dokka

Every public API element needs useful KDoc. Add KDoc to classes, interfaces,
objects, enums, constructors, functions, properties, and thrown exceptions.
Cross-link to user-facing guides with `@see` where appropriate.

Internal members need only enough KDoc to explain intent.

## 6. Add configuration and wiring

Update `modules/<module>/build.gradle.kts` if the implementation needs new
dependencies, source sets, or tasks:

- Prefer `implementation` over `api`.
- Do not expose implementation dependencies as public API.
- Use version catalogs (`libs.xxx`).

## 7. Run the full verification suite

Verify the module builds and quality checks pass:

```bash
./gradlew :<module>:build :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply`, then review the diff.

If the module publishes API docs:

```bash
./gradlew :<module>:dokkaHtml
```

Review Dokka output to confirm public API docs are complete and links work.

## 8. Review before committing

Check the implementation against project standards:

- **Readable**: names reveal intent; complex logic is split into helpers.
- **Tested**: every new behavior is covered; edge cases are present.
- **Documented**: public API has KDoc; internal code is self-explanatory.
- **Consistent**: follows the style enforced by Spotless.
- **Secure**: inputs are validated; secrets are not logged or exposed.

