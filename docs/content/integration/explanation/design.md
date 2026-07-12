# Integration Design

Terracotta separates integration guides from module docs so each page can focus on a single concern: module docs explain APIs, integration docs explain wiring.

## Why integration docs are separate

A user who wants to publish a Paper plugin to Hangar does not need to understand the Hangar provider SPI or the Gradle plugin task lifecycle. They need a single workflow that covers authentication, configuration, and the publish command. If that workflow were split across module docs, the user would have to understand each module before accomplishing anything.

By keeping integration docs separate:

- **Module docs stay focused.** The Gradle plugin docs explain DSL and tasks without repeating registry-specific setup. The provider docs explain registry behavior without repeating Gradle basics.
- **Workflows are discoverable.** A user can find "publish to Modrinth" without knowing which module owns which step.
- **Examples stay realistic.** Integration pages can show complete `terracotta.yml` snippets and environment setup, which would be out of place in API-focused module docs.

## How multi-provider publishing works

Terracotta treats each provider as an independent target. When you run `terracottaApply`:

1. Terracotta reads local state from `terracotta.yml`, `build.gradle.kts`, and detected project files.
2. For each configured provider, it fetches the current remote state.
3. It computes a diff between local and remote state for each provider independently.
4. It applies the resulting operations provider by provider.

Providers do not know about each other. A failure in one provider does not roll back changes in another. This design keeps providers isolated and lets you add a new registry without rethinking existing ones.

## When to read module docs instead

Integration guides are the right starting point for most users. Read module docs when you need to:

- Implement a custom provider. See [Implement a Custom Provider](../../modules/core/tutorials/implementing-a-custom-provider.md).
- Understand the DSL in depth. See [Kotlin DSL Configuration](../../modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md).
- Debug provider-specific behavior. See the [Modrinth provider](../../modules/provider-modrinth/README.md) or [Hangar provider](../../modules/provider-hangar/README.md) docs.
