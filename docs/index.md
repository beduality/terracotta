# Terracotta

Terracotta is a declarative project registry management tool for Minecraft developers. It manages registry providers like Modrinth project metadata, description, tags, and versions using a simple YAML configuration file as the single source of truth.

---

<div class="grid cards" markdown>

-   :material-file-document-edit-outline:{ .lg .middle } __Declarative Configuration__

    ---

    Define your project info, description path, categories, tags, license, and version artifacts in `terracotta.yaml`.

    [:octicons-arrow-right-24: Getting started](content/tutorials/getting-started.md)

-   :material-eye-outline:{ .lg .middle } __Dry Runs (Plan)__

    ---

    Preview all updates, tag changes, and version uploads before executing them, preventing incorrect uploads or metadata state drift.

    [:octicons-arrow-right-24: CLI commands](content/reference/cli.md)

-   :material-publish:{ .lg .middle } __Maven SDK__

    ---

    Integrate Terracotta's core directly into Gradle/Maven plugins, IDEs, or custom automation pipelines without the CLI.

    [:octicons-arrow-right-24: Core API](content/reference/api.md)

-   :material-sync:{ .lg .middle } __CI/CD Integration__

    ---

    Easily integrate with GitHub Actions to apply registry changes automatically when you push tags or release artifacts.

    [:octicons-arrow-right-24: CI/CD Guide](content/how-to-guides/ci-cd-setup.md)

</div>

## How It Works

1. You edit `terracotta.yaml` in your project root.
2. Run `terracotta plan` to view a diff of local configuration vs remote Modrinth state.
3. Run `terracotta apply` to push changes (metadata, upload versions, synchronize tags) to the remote registry.

```text
~ Update description
~ Update tags (from: utility to: utility, paper)
+ Upload version 1.2.0
```

## Setup Requirements

| Component | Version |
|---|---|
| JVM / JDK | 21+ |
| Native Image | GraalVM (for native executables) |
| Target Registries | Modrinth API (Hangar/CurseForge planned) |

## Links

- [:fontawesome-brands-github: GitHub](https://github.com/beduality/terracotta)
- [:fontawesome-brands-discord: Discord](https://discord.gg/D5meCv2Wnd)
