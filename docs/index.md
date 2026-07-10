# Terracotta

Terracotta is a declarative project registry management tool for Minecraft developers. It manages registry providers like Modrinth project metadata, description, tags, and versions using a simple Gradle configuration as the single source of truth.

---

<div class="grid cards" markdown>

-   :material-file-document-edit-outline:{ .lg .middle } __Declarative Configuration__

    ---

    Define your project info, description, tags, license, and version artifacts in your `build.gradle.kts`.

    [:octicons-arrow-right-24: Getting started](content/gradle-plugin/tutorials/getting-started.md)

-   :material-eye-outline:{ .lg .middle } __Dry Runs (Plan)__

    ---

    Preview all updates, tag changes, and version uploads before executing them, preventing incorrect uploads or metadata state drift.

    [:octicons-arrow-right-24: Gradle tasks](content/gradle-plugin/reference/tasks.md)

-   :material-publish:{ .lg .middle } __Maven SDK__

    ---

    Integrate Terracotta's core directly into custom automation pipelines.

    [:octicons-arrow-right-24: Core API](content/sdk/reference/api.md)

-   :material-sync:{ .lg .middle } __CI/CD Integration__

    ---

    Easily integrate with GitHub Actions to apply registry changes automatically when you push tags or release artifacts.

    [:octicons-arrow-right-24: CI/CD Guide](content/gradle-plugin/how-to-guides/ci-cd-setup.md)

</div>

## How It Works

1. You configure Terracotta in your `build.gradle.kts`.
2. Run `./gradlew terracottaPlan` to view a diff of local configuration vs remote Modrinth state.
3. Run `./gradlew terracottaApply` to push changes (metadata, upload versions, synchronize tags) to the remote registry.

Example output from `terracottaPlan`:

```text
~ Update summary (from: "Old summary" to: "Lightweight Paper plugin")
~ Update tags (from: ["utility"] to: ["utility", "paper"])
+ Upload version 1.2.0 (file: build/libs/my-plugin-1.2.0.jar)
```

## Setup Requirements

| Component | Version |
|---|---|
| JVM / JDK | 17+ |
| Target Registries | Modrinth API (Hangar/CurseForge planned) |

## Links

- [:fontawesome-brands-github: GitHub](https://github.com/beduality/terracotta)
- [:fontawesome-brands-discord: Discord](https://discord.gg/D5meCv2Wnd)
