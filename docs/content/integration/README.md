# Integration

Integration guides explain how to wire Terracotta modules and external tools together to publish real projects.

These pages are separate from module-specific docs so that module docs stay focused on APIs and concepts, while integration docs cover end-to-end workflows.

## Available modules

These Terracotta modules are used when wiring up integrations:

- **[Modrinth Provider](../modules/provider-modrinth/README.md)** — provider implementation for the [Modrinth](https://modrinth.com/) registry.
- **[Hangar Provider](../modules/provider-hangar/README.md)** — provider implementation for the [Hangar](https://hangar.papermc.io/) registry.
- **[State Filesystem](../modules/terracotta-state-filesystem/README.md)** — file-backed state backend that persists run state to a local YAML file.

## Quick links

- [Publish to Modrinth and Hangar](tutorials/publishing-to-multiple-providers.md)
- [Add Modrinth to the Gradle plugin](how-to-guides/adding-modrinth-to-gradle-plugin.md)
- [Add Hangar to the Gradle plugin](how-to-guides/adding-hangar-to-gradle-plugin.md)
- [Troubleshoot provider integration](how-to-guides/troubleshooting.md)
- [Provider configuration](reference/provider-configuration.md)
- [Integration design](explanation/design.md)
