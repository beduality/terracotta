---
description: Reusable workflow for designing a new or redesigned Terracotta module before implementation, tests, or docs.
---

# Module System Design Workflow

A repeatable workflow for designing a `terracotta-*` module. This phase happens
before writing tests, implementation, or documentation. The output is a stable
mental model and a written design proposal that the team can review.

This is Phase 1 of the module development workflow. Start with
`module-development-workflow.md` if you have not read it.

## 1. Define the problem and scope

Start by writing down what the module is responsible for and, just as
importantly, what it is not responsible for.

- What user need or system gap does the module address?
- Which existing modules does it interact with?
- What is out of scope for this module?
- Check `project/TODO.md` and `project/BACKLOG.md` for related work before defining scope.

Aim for one or two tight responsibilities. If a module has too many reasons to
change, split it.

## 2. Identify inputs, outputs, and side effects

List every external interaction the module will have:

- **Inputs**: data, configuration, events, files, environment variables, CLI arguments.
- **Outputs**: data returned, files written, network calls made, tasks registered.
- **Side effects**: logging, caching, state mutation, external service calls.

Map these onto a boundary diagram or a short table. This makes the module's
surface area explicit before any code is written.

## 3. Design the public API contract

Design the API that other modules and consumers will use. Do not write
implementations yet; focus on signatures and contracts.

- Entry points: classes, functions, DSL objects, tasks, or CLI commands.
- Data types: request/response models, configuration objects, enums, sealed classes.
- Extension points: interfaces or abstract types that allow future variants.
- Error contract: exceptions, result types, or error codes consumers will receive.

Keep the surface small. Every public symbol is a long-term commitment.

## 4. Model the internal architecture

Inside the module, define the major components and how they collaborate.

- **Ports**: interfaces that describe how the module talks to the outside world.
- **Adapters**: concrete implementations of ports for specific libraries or services.
- **Domain services**: pure business logic with no framework dependencies.
- **Configuration**: how the module is initialized and wired together.

Prefer dependency injection and composition over static state. Keep framework
code at the edges, not at the center of the domain.

## 5. Define module boundaries

State what this module will never depend on and what other modules should never
know about its internals.

- A module should depend only on modules that are more stable or more abstract.
- Avoid circular dependencies between modules.
- Mark internal packages and symbols as `internal` in Kotlin.
- Keep provider-specific or framework-specific types out of the core API.

Use a dependency direction rule such as:

```
app / CLI → module API → domain services → ports → adapters → external libraries
```

## 6. Decide composability and extensibility mechanisms

Choose how the module can be extended without changing its core:

| Mechanism | Use when |
|-----------|----------|
| Interface / abstract class | Multiple implementations of the same capability |
| Sealed class | A closed set of variants known to the module |
| Strategy object | A single behavior that can be swapped |
| Listener / callback | Optional reactions to lifecycle events |
| Plugin / provider registry | External modules contribute implementations |

Avoid inheritance-heavy designs. Prefer composition and small, focused interfaces.

## 7. Choose types for type safety

Use Kotlin's type system to make invalid states unrepresentable.

- Use value classes for domain identifiers (e.g., `ProjectId`, `VersionString`).
- Use sealed classes for state machines and result types.
- Use `data class` for configuration and plain data.
- Avoid `Any`, raw strings, and booleans that carry hidden meaning.

Define where null is acceptable and where it is a bug. Default to non-null types.

## 8. Plan error handling

Design how errors are reported and recovered.

- Distinguish between programmer errors, configuration errors, and external failures.
- Prefer typed result objects or domain-specific exceptions over generic exceptions.
- Decide whether a failure is retryable, fatal, or recoverable by the caller.
- Document which entry points can throw and under what conditions.

## 9. Plan configuration and initialization

Design how consumers create and configure the module.

- Constructor-based configuration is preferred over mutable builders.
- Use sensible defaults so the module works out of the box for common cases.
- Validate configuration eagerly and report all problems at once when possible.
- Avoid global singletons; allow multiple independent instances.

## 10. Evaluate against quality attributes

Review the design against the attributes that matter for production software:

- **Correctness**: does the design satisfy the requirements?
- **Testability**: can components be tested in isolation?
- **Composability**: can parts be reused or replaced?
- **Modularity**: are responsibilities clearly separated?
- **Extensibility**: can new variants be added without core changes?
- **Type safety**: does the type system prevent misuse?
- **Performance**: are expensive operations located and justified?
- **Observability**: can operators understand failures and state?

Update the design where the evaluation reveals gaps.

## 11. Write the design proposal

Capture the design in a short proposal under `project/proposals/` or in the
issue tracker. Include:

- Problem statement and scope.
- Public API sketch (Kotlin signatures or pseudo-code).
- Internal component diagram or list.
- Module boundary and dependency rules.
- Extension points and error handling strategy.
- Open questions and risks.

Keep it concise. The proposal is a tool for discussion, not a specification
that replaces code.

## 12. Review before proceeding

Do not start implementation until the design has been reviewed.

- Walk through the public API with a consumer in mind.
- Check that the design does not duplicate responsibilities of other modules.
- Confirm the dependency graph has no cycles.
- Ensure the error contract is clear.

Once the proposal is accepted, the module is ready for the TDD workflow, then the
implementation workflow, then the documentation workflow.

---

## Module placeholders

For shared placeholders, see `README.md` in this directory.

Phase-specific placeholders:

| Placeholder | Example value |
|-------------|---------------|
| Proposal location | `project/proposals/<module>-design.md` |

---

## Related files

- `README.md` — shared placeholders and commit/release guidance.
- `project/proposals/` — design proposals for modules and major features.
- `project/TODO.md` and `project/BACKLOG.md` — active work and backlog.
- `settings.gradle.kts` — module inclusion and naming.
- `modules/<module>/build.gradle.kts` — module dependencies and plugins.
- `gradle/libs.versions.toml` — shared dependency versions.
