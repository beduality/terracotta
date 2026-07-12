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
    icon.set(file("docs/assets/icon.png"))

    gallery {
        register("screenshot") {
            imageFile.set(file("docs/assets/screenshot.png"))
            title.set("Main inventory screen")
            description.set("Shows the new GUI")
            featured.set(true)
            ordering.set(0)
        }
    }

    links {
        homepage.set("https://example.com/my-plugin")
        source.set("https://github.com/example/my-plugin")
        issues.set("https://github.com/example/my-plugin/issues")
        wiki.set("https://github.com/example/my-plugin/wiki")
        community.set("https://discord.gg/example")
        donation("ko-fi", "https://ko-fi.com/example")
        other("twitter", "https://twitter.com/example")
    }

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

## Gallery configuration

You can declare gallery images in `terracotta.yml`, in `build.gradle.kts`, or both. Items from `terracotta.yml` are loaded first, and DSL registrations are added on top.

```kotlin
terracotta {
    gallery {
        register("screenshot") {
            imageFile.set(file("docs/assets/screenshot.png"))
            title.set("Main inventory screen")
            description.set("Shows the new GUI")
            featured.set(true)
            ordering.set(0)
        }
    }
}
```

The `title` is used as the stable identity key: if you rename an image, Terracotta will delete the old image and upload a new one.

## Links configuration

You can configure canonical project links in the DSL. Values set here override links from `terracotta.yml`, and donations or `other` entries are merged with YAML entries.

```kotlin
terracotta {
    links {
        homepage.set("https://example.com/my-plugin")
        source.set("https://github.com/example/my-plugin")
        issues.set("https://github.com/example/my-plugin/issues")
        wiki.set("https://github.com/example/my-plugin/wiki")
        community.set("https://discord.gg/example")
        donation("ko-fi", "https://ko-fi.com/example")
        other("twitter", "https://twitter.com/example")
    }
}
```

`homepage`, `source`, `issues`, `wiki`, and `community` accept `Property<String>`. `donation(platform, url)` appends a donation link, and `other(label, url)` appends an arbitrary label-to-URL entry.

## State backend

The Gradle plugin persists run state through a pluggable backend. The default backend is `"filesystem"`, which writes to `.terracotta-state.yml` in the project directory.

```kotlin
terracotta {
    stateSource.set("filesystem")
    stateSourceSettings.put("path", "custom-state.yml")
}
```

- `stateSource` selects the backend by its factory `id` (default: `"filesystem"`).
- `stateSourceSettings` is a `MapProperty<String, String>` with backend-specific settings. The filesystem backend recognizes `path`.

State files are generated at build time and should not be committed; see [State Management](../../core/explanation/state-management.md) for details.

### Deprecated `stateFile`

The older `stateFile` property is deprecated but continues to work. Setting `stateFile` is equivalent to `stateSource = "filesystem"` with `stateSourceSettings["path"] = <file>`. Prefer `stateSource` and `stateSourceSettings` for new configuration.

---

See the [Config Schema](../../core/reference/config-schema.md) for a complete list of available fields.
