# Configure Terracotta with the Kotlin DSL

Instead of using `terracotta.yml`, you can configure Terracotta directly in `build.gradle.kts`. This is useful when you want to compute values dynamically or keep everything in one file.

## Basic configuration

```kotlin
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

terracotta {
    name.set("My Plugin")
    summary.set("Lightweight Paper plugin")
    description.set(file("README.md").readText())
    tags.set(listOf("paper", "utility"))
    license.set("MIT")
    gameVersions.set(listOf("1.21.8", "1.21.7"))
    loaders.set(listOf("paper"))
    environment.set(TerracottaEnvironment.SERVER_ONLY)
    releaseType.set(TerracottaReleaseType.RELEASE)
    changelog.set("Initial release")

    providers {
        create("modrinth") {
            projectId.set("my-modrinth-project-id")
            token.set(System.getenv("MODRINTH_TOKEN"))
        }
        create("hangar") {
            projectId.set("my-hangar-project-slug")
            token.set(System.getenv("HANGAR_TOKEN"))
        }
    }
}
```

## When to use the Kotlin DSL

- You need to compute values from other Gradle tasks or files.
- You prefer keeping configuration alongside the build logic.
- You want to compute loader IDs or other values dynamically from your build.

## Mixing YAML and Kotlin DSL

You can use `terracotta.yml` for shared metadata and override only specific values in `build.gradle.kts`:

```yaml
# terracotta.yml
name: "My Plugin"
providers:
  modrinth:
    projectId: "my-modrinth-project-id"
```

```kotlin
// build.gradle.kts
terracotta {
    description.set(file("README.md").readText())
    gameVersions.set(listOf("1.21.8"))
}
```

Values set in the Kotlin DSL always override values from `terracotta.yml`.

---

See the [Config Schema](../../core/reference/config-schema.md) for a complete list of available fields.
