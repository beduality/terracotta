# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [MIT License](./LICENSE) | [Contributing](./CONTRIBUTING.md)

Declarative Minecraft project registry management tool. Define your project metadata, description, tags, and version artifacts in a single `terracotta.yaml` and sync them to registries like Modrinth.

## Installation

### CLI Installation

Download compiled native binaries from the [Releases](https://github.com/beduality/terracotta/releases) page.

**Linux & macOS:**
```bash
chmod +x terracotta
mv terracotta /usr/local/bin/
```

**Windows:**
Download `terracotta-windows-amd64.exe` and add it to your PATH.

### SDK Installation

Add the Terracotta Core library to your project:

**Gradle (Kotlin):**
```kotlin
implementation("io.github.beduality:terracotta-core:0.1.0")
```

**Maven:**
```xml
<dependency>
    <groupId>io.github.beduality</groupId>
    <artifactId>terracotta-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

For the Modrinth provider:
```kotlin
implementation("io.github.beduality:terracotta-provider-modrinth:0.1.0")
```

See [SDK Installation Documentation](https://beduality.github.io/terracotta/tutorials/installation_sdk/) for more details.

## Usage

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

## Project Structure

The codebase is organized as a multi-project Gradle build under the `modules/` directory:

- **[terracotta-core](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-core)**: Pure domain library containing the canonical models, provider interfaces, and semantic diff engine. Published to Maven Central (`io.github.beduality:terracotta-core`).
- **[terracotta-provider-modrinth](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth)**: Modrinth state and registry providers implementation using OkHttp and Jackson.
- **[terracotta-cli](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-cli)**: Command line frontend using Picocli, compiled to native binaries using GraalVM.
- **[terracotta-github](file:///home/luis/GitHub/beduality/terracotta/modules/terracotta-github)**: Pulumi infrastructure project in Kotlin using Java SDK to manage GitHub repository settings and Action secrets.

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
