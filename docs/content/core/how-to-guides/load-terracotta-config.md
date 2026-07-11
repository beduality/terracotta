# Load a `terracotta.yml` File

Load a `terracotta.yml` configuration file into a `TerracottaConfig` object.

## Preconditions

- `terracotta-core` is on the classpath.
- A `terracotta.yml` file exists or you want an empty default config.

## Steps

1. Construct a `File` pointing to the configuration.
2. Call `TerracottaConfigLoader.load(file)`.

```kotlin
import io.github.beduality.terracotta.core.config.TerracottaConfigLoader
import java.io.File

val config = TerracottaConfigLoader.load(File(projectDir, "terracotta.yml"))
```

## Behavior

- If the file does not exist, the loader returns an empty `TerracottaConfig` with all fields set to their default values.
- Partial files are valid; missing fields remain `null` or empty.
- Lists are read as YAML sequences of strings.
- Provider and convention blocks are read as nested maps.

## Outcome

You have a `TerracottaConfig` instance that can be passed to `ProjectMetadataResolver`.

## See also

- [Resolve Project Metadata](resolve-project-metadata.md)
- [Config Schema](../reference/config-schema.md)
