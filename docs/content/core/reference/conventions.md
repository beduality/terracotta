# Conventions

Conventions interpret project files such as `README.md` and `CHANGELOG.md` so that metadata detectors do not need to know every file format.

## Readme conventions

| ID | Description |
|----|-------------|
| `terracotta` | Full trimmed content is the description; first non-heading paragraph is the summary. |

## Changelog conventions

| ID | Description |
|----|-------------|
| `keep-a-changelog` | Extracts the body under a `## [version]` heading. |

## Registration

Built-in conventions are registered automatically when `ProjectFileConventionRegistry.load()` is called. Additional conventions can be registered at runtime:

```kotlin
ProjectFileConventionRegistry.register(MyReadmeConvention)
```

## Convention identifiers

Identifiers are matched case-insensitively. Unknown identifiers cause `IllegalArgumentException`.

## See also

- [Add a Project-File Convention](../how-to-guides/add-a-new-project-file-convention.md)
- [Conventions Explanation](../explanation/conventions.md)
- [API Documentation](api.md)
