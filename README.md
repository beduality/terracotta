# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [MIT License](./LICENSE)

Terracotta is a declarative Minecraft project registry management tool written in Kotlin. It manages your Modrinth project metadata, description, tags, and version artifacts using a single configuration file (`terracotta.yaml`) as the single source of truth.

---

## Project Structure

The codebase is organized as a multi-project Gradle build under the `modules/` directory:

- **[terracotta-core](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-core)**: Pure domain library containing the canonical models, provider interfaces, and semantic diff engine. Published to Maven Central (`io.github.beduality:terracotta-core`).
- **[terracotta-provider-modrinth](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth)**: Modrinth state and registry providers implementation using OkHttp and Jackson.
- **[terracotta-cli](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-cli)**: Command line frontend using Picocli, compiled to native binaries using GraalVM.

---

## Getting Started

1. Create a `terracotta.yaml` in your project root:
   ```yaml
   project:
     id: my-plugin
     name: My Plugin
     summary: Lightweight Paper plugin
   description: README.md
   license: MIT
   ```
2. Run a dry run to generate a plan:
   ```bash
   terracotta plan
   ```
3. Apply the changes:
   ```bash
   terracotta apply
   ```

---

## Development Workflow

To build and compile Terracotta:

- Run unit tests:
  ```bash
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
  ```
- Build the native binary (requires GraalVM setup):
  ```bash
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew nativeCompile
  ```
