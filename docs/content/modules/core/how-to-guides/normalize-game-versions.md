# Normalize Game Versions

Convert a raw game-version string into the canonical identifiers that Terracotta accepts.

## Preconditions

- `terracotta-core` is on the classpath.
- You have a raw version string such as `>=1.21.1` or `[1.20.1,1.21.1]`.

## Steps

1. Resolve the default Minecraft game-version convention.
2. Call `parse`.

```kotlin
import io.github.beduality.terracotta.core.model.version.GameVersionConventionResolver

val convention = GameVersionConventionResolver.resolve(null)
val canonical = convention.parse(">=1.21.1")
```

## Behavior

The default convention accepts:

- Classic releases: `1.20.1`
- Snapshots: `25w14a`
- Pre-releases: `1.21.5-pre1`
- Release candidates: `1.21.5-rc1`

It strips dependency operators and surrounding brackets automatically.

## Outcome

You have a canonical lowercase version identifier suitable for use in `TerracottaVersion.gameVersions`.

## Variants

- To normalize a larger block of text and extract all valid versions, use `GameVersionNormalizer`.

```kotlin
import io.github.beduality.terracotta.core.detect.adapters.GameVersionNormalizer

val versions = GameVersionNormalizer.normalize("[1.20.1, 1.21.1]")
```

## See also

- [Version Conventions Reference](../reference/version-conventions.md)
