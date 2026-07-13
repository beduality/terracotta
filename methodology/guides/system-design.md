---
description: Reusable workflow for designing a new or redesigned Terracotta module before implementation, tests, or docs.
---

# Module System Design Workflow

A repeatable workflow for designing a `terracotta-*` module before tests,
implementation, or documentation. Output is a stable mental model and a
reviewable design proposal.

This is Phase 2 of the module development workflow. Start with
`development.md` if you have not read it.

## 1. Define the problem and scope

Write down what the module is responsible for and what it is not:

- User need or system gap it addresses.
- Existing modules it interacts with.
- What is out of scope.
- Related work in `project/TODO.md` and `project/BACKLOG.md`.

Aim for one or two tight responsibilities; split modules that have too many reasons to change.

## 2. Identify inputs, outputs, and side effects

List inputs, outputs, and side effects. Map them onto a short table or boundary
diagram to make the module's surface explicit.

## 3. Design the public API contract

Design the API consumers will use. Focus on signatures and contracts, not
implementations:

- Entry points: classes, functions, DSL objects, tasks, CLI commands.
- Data types: models, configuration, enums, sealed classes.
- Extension points: interfaces or abstract types for future variants.
- Error contract: exceptions, result types, error codes.

Keep the surface small; every public symbol is a long-term commitment.

## 4. Model the internal architecture

Define major internal components and how they collaborate:

- **Ports**: interfaces for outside-world interaction.
- **Adapters**: concrete port implementations for specific libraries or services.
- **Domain services**: pure business logic, no framework dependencies.
- **Configuration**: initialization and wiring.

Prefer dependency injection and composition. Keep framework code at the edges.

## 5. Define module boundaries

State what this module will never depend on and what other modules should never
know about its internals.

- Depend only on more stable or more abstract modules.
- Avoid circular dependencies.
- Mark internal packages and symbols as `internal`.
- Keep provider- or framework-specific types out of the core API.

Dependency direction:

```
app / CLI → module API → domain services → ports → adapters → external libraries
```

## 6. Decide composability and extensibility mechanisms

Choose extension mechanisms without changing core code:

| Mechanism | Use when |
|-----------|----------|
| Interface / abstract class | Multiple implementations of the same capability |
| Sealed class | A closed set of variants |
| Strategy object | A single swappable behavior |
| Listener / callback | Optional reactions to lifecycle events |
| Plugin / provider registry | External modules contribute implementations |

Prefer composition and small, focused interfaces.

## 7. Choose types for type safety

Use Kotlin's type system to make invalid states unrepresentable:

- Value classes for domain identifiers (`ProjectId`, `VersionString`).
- Sealed classes for state machines and result types.
- `data class` for configuration and plain data.
- Avoid `Any`, raw strings, and booleans with hidden meaning.

Default to non-null types; define where null is acceptable.

## 8. Plan error handling

Design error reporting and recovery:

- Distinguish programmer, configuration, and external failures.
- Prefer typed results or domain exceptions over generic exceptions.
- Decide if a failure is retryable, fatal, or recoverable.
- Document which entry points can throw and when.

## 9. Plan configuration and initialization

Design creation and configuration:

- Prefer constructor-based configuration over mutable builders.
- Use sensible defaults.
- Validate eagerly and report all problems at once when possible.
- Avoid global singletons; allow multiple independent instances.

## 10. Evaluate against quality attributes

Review against production attributes and update where gaps appear:

- **Correctness**, **testability**, **composability**, **modularity**.
- **Extensibility**, **type safety**, **performance**, **observability**.

## 11. Write the design proposal

Capture the design in a short proposal under `project/designs/<module>-design.md`. Include:

- Problem statement and scope.
- Public API sketch.
- Internal component diagram or list.
- Module boundary and dependency rules.
- Extension points and error-handling strategy.
- Open questions and risks.

The proposal is a discussion tool, not a replacement for code.

## 12. Review before proceeding

Do not start implementation until the design is reviewed:

- Walk through the public API with a consumer in mind.
- Check for duplicated responsibilities.
- Confirm the dependency graph has no cycles.
- Ensure the error contract is clear.

Once accepted, move to testing, implementation, and documentation.

