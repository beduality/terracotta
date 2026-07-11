# Version Conventions

Version conventions parse and validate version strings.

## Project version convention

| ID | Description |
|----|-------------|
| `semver` | Semantic Versioning 2.0.0. Tolerates a leading `v` or `V`. |

Use `VersionConventionResolver.versionConvention(id)` to resolve a convention.

## Game version convention

| ID | Description |
|----|-------------|
| `minecraft` | Parses Minecraft releases, snapshots, pre-releases, and release candidates. |

Use `GameVersionConventionResolver.resolve(id)` to resolve a convention.

## Accepted Minecraft formats

The default convention accepts strings such as:

| Input | Canonical output |
|-------|------------------|
| `1.21.1` | `1.21.1` |
| `1.21.5-pre1` | `1.21.5-pre1` |
| `1.21.5-rc1` | `1.21.5-rc1` |
| `25w14a` | `25w14a` |
| `>=1.21.1` | `1.21.1` |
| `[1.20.1,1.21.1]` | `1.20.1`, `1.21.1` |

Dependency operators and brackets are stripped during parsing.

## Normalization

`GameVersionNormalizer` scans an arbitrary block of text and returns all valid Minecraft version substrings.

## See also

- [Normalize Game Versions](../how-to-guides/normalize-game-versions.md)
- [API Documentation](api.md)
