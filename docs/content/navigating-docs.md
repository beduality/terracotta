# Navigating the Docs

Terracotta's documentation is organized around what you want to do, not around the codebase. The structure follows the [Diátaxis](https://diataxis.fr/) framework, where every page has one of four goals:

| Type | Question it answers | Example |
|---|---|---|
| **Tutorial** | How do I learn this? | [Getting Started](modules/gradle-plugin/tutorials/getting-started.md) |
| **How-To Guide** | How do I do X? | [Adding Modrinth to the Gradle Plugin](integration/how-to-guides/adding-modrinth-to-gradle-plugin.md) |
| **Reference** | What are the exact details? | [Config Schema](modules/core/reference/config-schema.md) |
| **Explanation** | Why does it work this way? | [Core Architecture](modules/core/explanation/architecture.md) |

## Top-level sections

- **[Quick Start](../index.md)**: Get from zero to a published release as fast as possible.
- **[Integration](integration/README.md)**: Add Terracotta to a real project, starting with the Gradle plugin and providers.
- **[Modules](modules/overview.md)**: Deep dives into each component — Core, Gradle Plugin, Modrinth Provider, and Hangar Provider.
- **[Repo](repo/README.md)**: Build, test, contribute, and release Terracotta itself.

## Where to go next

- If you are new, follow [Getting Started](modules/gradle-plugin/tutorials/getting-started.md).
- If you want to understand the `terracotta.yml` format, see [Config Schema](modules/core/reference/config-schema.md).
- If you want to add a new registry or build integration, read [Implement a Custom Provider](modules/core/tutorials/implementing-a-custom-provider.md).
