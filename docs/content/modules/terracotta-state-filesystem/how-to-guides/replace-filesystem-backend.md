# Replace the Filesystem Backend

The Terracotta Gradle plugin depends on `terracotta-state-filesystem` by default, so the `"filesystem"` backend works out of the box. You can exclude it if you want a smaller classpath or if you are providing your own state backend.

## Exclude the filesystem backend

Use Gradle's dependency management to remove the module from the plugin classpath:

```kotlin
buildscript {
    dependencies {
        classpath("io.github.beduality:terracotta-gradle-plugin:<version>") {
            exclude(group = "io.github.beduality", module = "terracotta-state-filesystem")
        }
    }
}
```

If you use the plugins DSL, exclude the dependency through `buildscript` instead, because the plugin resolution mechanism does not support per-plugin excludes.

## What happens when it is missing

If you do not add a replacement backend and `terracotta.stateSource` still defaults to `"filesystem"`, the build fails during configuration with a clear error:

```
No state source factory found with id 'filesystem'. Available factories: [].
Make sure the backend module is on the classpath. For the default filesystem backend, add:
  implementation("io.github.beduality:terracotta-state-filesystem:<version>")
```

Restore the backend by adding the dependency back to the classpath.

## Add a custom backend

Implement the state SPI in your own module or buildscript:

1. Add `terracotta-core` as a dependency.
2. Implement `StateSourceFactory` and `StateSource`.
3. Register the factory in `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory`.
4. Put the JAR on the plugin classpath alongside `terracotta-gradle-plugin`.
5. Set `terracotta.stateSource = "<your-factory-id>"` and any required `terracotta.stateSourceSettings`.

See the [State Management explanation](../../core/explanation/state-management.md) for the SPI contract and the [Kotlin DSL configuration guide](../../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md) for Gradle-specific configuration.

## Use a custom state file path

You do not need to exclude the filesystem backend to change the state file path. Keep the default backend and override the path:

```kotlin
terracotta {
    stateSource = "filesystem"
    stateSourceSettings["path"] = "state/terracotta.yml"
}
```

Relative paths are resolved against the project directory.
