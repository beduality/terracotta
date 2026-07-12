# Repository Architecture

Terracotta is split into focused modules so that users, providers, and build-tool integrations can evolve independently.

## Why modules?

A single module would force every consumer to depend on everything. A Gradle plugin user would pull in Modrinth and Hangar clients; a core SDK user would depend on Gradle APIs. Splitting the codebase keeps each artifact small and lets each module follow its own release cadence.

The repository contains five Gradle modules:

| Module | Responsibility | Publishes to Maven Central |
|---|---|---|
| `terracotta-core` | Domain models, metadata resolution, diff engine, provider SPI | Yes |
| `terracotta-provider-modrinth` | Modrinth registry integration | Yes |
| `terracotta-provider-hangar` | Hangar registry integration | Yes |
| `terracotta-gradle-plugin` | Gradle DSL, tasks, and build integration | Yes |
| `terracotta-github` | Pulumi infrastructure for repository settings and secrets | No |

## `terracotta-core`

The center of the system. It knows nothing about Gradle, Modrinth, Hangar, or HTTP. It defines:

- Canonical models for projects, versions, loaders, and game versions.
- The provider SPI that registry integrations implement.
- Metadata resolution rules that combine explicit config, detected files, and defaults.
- The diff engine that turns local and remote state into semantic operations.

Keeping this module pure makes it testable without network access and reusable by non-Gradle consumers.

## Provider modules

`terracotta-provider-modrinth` and `terracotta-provider-hangar` implement the provider SPI for their respective registries. Each one owns:

- Registry-specific API clients.
- State mapping between Terracotta models and registry models.
- Provider factories discovered at runtime through `ServiceLoader`.

This split means a new registry can be added without touching core or the Gradle plugin.

## `terracotta-gradle-plugin`

The plugin bridges the build and the core library. It:

- Exposes a `terracotta` DSL in `build.gradle.kts`.
- Discovers available providers through `ServiceLoader`.
- Adds `terracottaPlan` and `terracottaApply` tasks.
- Reads project metadata from Gradle itself.

The plugin deliberately does not know registry details. It depends on `terracotta-core` and whichever provider modules are on the classpath.

## `terracotta-github`

This module contains Pulumi code for managing GitHub repository configuration and Actions secrets. It does not ship as a library; it is run manually by maintainers when repository settings need to change. Keeping it in the same repository ensures the infrastructure definition is versioned alongside the code it supports.

## Why Pulumi for secrets?

Terracotta needs Sonatype and GPG secrets for releases. Storing them in the GitHub UI is fragile and hard to audit. Pulumi lets us:

- Declare repository settings and secrets as code.
- Review changes through PRs.
- Reproduce the repository configuration if needed.

Secrets are loaded from a local `.env` file by `scripts/load_pulumi_secrets.py`, encrypted in Pulumi config, and provisioned as GitHub Actions secrets.

## Documentation as a first-class concern

Docs live in `docs/` rather than inside each module because many pages cross module boundaries. The integration guides, for example, discuss both the Gradle plugin and providers. A dedicated docs tree makes the site structure explicit and lets the release pipeline deploy versioned documentation that matches the released code.

## Planning outside `main`

Planning files live on a separate `project` branch checked out as a worktree. This keeps `main` focused on shippable work while preserving an auditable history of decisions. For more on this, see [Branch Strategy](branch-strategy.md) and [Project Management](project-management.md).
