# Modrinth Version Mapping

Terracotta versions are mapped to Modrinth versions one-to-one.

## Version fields

| Terracotta | Modrinth |
|---|---|
| `version` | `version_number` |
| `gameVersions` | `game_versions` |
| `loaders` | `loaders` |
| `releaseType` | `version_type` |
| `changelog` | `changelog` |
| `artifactPath` (first file) | primary file |

## Release type mapping

| `TerracottaReleaseType` | Modrinth `version_type` |
|---|---|
| `RELEASE` | `release` |
| `BETA` | `beta` |
| `ALPHA` | `alpha` |

## File handling

The provider reads the artifact at `artifactPath` and uploads it as the primary file for the Modrinth version. If the file is missing, the upload fails before any network request is made.
