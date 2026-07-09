# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [Contributing](./CONTRIBUTING.md) | [MIT License](./LICENSE)

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
