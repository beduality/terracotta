# Quick Start

This tutorial will get you up and running with the Terracotta SDK in under 10 minutes.

## What you'll learn

- How to add the Terracotta SDK dependency to your project
- How to create a local project configuration
- How to compute differences between local and remote state
- How to apply changes to Modrinth

## Prerequisites

- Java/Kotlin project with Gradle or Maven
- JDK 21+
- Modrinth API token (optional, for writing)

## 1. Add the Dependency

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-core:0.1.0")
        implementation("io.github.beduality:terracotta-provider-modrinth:0.1.0")
    }
    ```

=== "Maven"

    ```xml
    <dependencies>
        <dependency>
            <groupId>io.github.beduality</groupId>
            <artifactId>terracotta-core</artifactId>
            <version>0.1.0</version>
        </dependency>
        <dependency>
            <groupId>io.github.beduality</groupId>
            <artifactId>terracotta-provider-modrinth</artifactId>
            <version>0.1.0</version>
        </dependency>
    </dependencies>
    ```

## 2. Set Up Your Token

Load your Modrinth API token from environment variables:

```kotlin
val token = System.getenv("MODRINTH_TOKEN")
    ?: throw IllegalStateException("MODRINTH_TOKEN environment variable not set")
```

For development, you can use a `.env` file with dotenv-kotlin:

```kotlin
implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
```

## 3. Create a Local Project

Define your project configuration:

```kotlin
import io.github.beduality.terracotta.core.model.*

val localProject = TerracottaProject(
    id = "my-mod",
    name = "My Awesome Mod",
    summary = "An awesome mod for Minecraft",
    description = "This mod adds cool features to Minecraft.",
    versions = listOf(
        TerracottaVersion(
            version = "1.0.0",
            artifactPath = "build/libs/my-mod-1.0.0.jar",
            gameVersions = listOf("1.21"),
            loaders = listOf(TerracottaLoader.FABRIC),
            environment = TerracottaEnvironment.CLIENT_ONLY,
        )
    ),
    tags = listOf("utility", "fun"),
    license = "MIT",
)
```

## 4. Fetch Remote State

Use the Modrinth provider to fetch the current remote state:

```kotlin
import io.github.beduality.terracotta.provider.modrinth.ModrinthProviderFactory
import kotlinx.coroutines.runBlocking

val factory = ModrinthProviderFactory()
val stateProvider = factory.createStateProvider(token)
val remoteProject = stateProvider.fetchProject(localProject.id)
```

## 5. Compute Differences

Use the diff engine to compute what needs to be changed:

```kotlin
import io.github.beduality.terracotta.core.diff.DiffEngine

val operations = DiffEngine.diff(localProject, remoteProject)
operations.forEach { println(it.description) }
```

## 6. Apply Changes

Upload changes to Modrinth:

```kotlin
val registryProvider = factory.createRegistryProvider(token)
registryProvider.apply(localProject.id, operations)
```

## What's Next?

- **[Provider API Reference](../reference/provider-api.md)**: Learn about all provider interfaces
- **[Modrinth Provider How-To](../how-to-guides/modrinth-provider.md)**: Detailed Modrinth integration guide
- **[Architecture Overview](../explanation/architecture.md)**: Understand how Terracotta works

## Troubleshooting

### "MODRINTH_TOKEN not set"

Make sure your environment variable is set:
```bash
export MODRINTH_TOKEN=your-token-here
```

### "Project not found"

If `remoteProject` is `null`, the project doesn't exist on Modrinth yet. Run `CreateProject` operation first.

### "Failed to apply operations"

Check that your token has the necessary permissions on Modrinth.