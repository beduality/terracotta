# Add a Project-File Convention

Register a custom convention for interpreting `README.md` or `CHANGELOG.md`.

## Preconditions

- `terracotta-core` is on the classpath.
- You have decided whether to implement `ReadmeConvention` or `ChangelogConvention`.

## Steps

1. Implement the convention interface.
2. Register it with `ProjectFileConventionRegistry` before resolution runs.

```kotlin
import io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.ProjectFileConventionRegistry

object BulletChangelogConvention : ChangelogConvention {
    override fun resolve(id: String): ChangelogConvention? {
        return if (id.equals("bullet", ignoreCase = true)) this else null
    }

    override fun extractVersionSection(content: String, version: String): String? {
        val escaped = Regex.escape(version)
        val pattern = Regex("""$escaped\n(.*?)(?=\n\d+\.\d+\.\d+\n|\Z)""", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(content)?.groupValues?.get(1)?.trim()
    }
}

ProjectFileConventionRegistry.load()
ProjectFileConventionRegistry.register(BulletChangelogConvention)
```

3. Reference the convention in `terracotta.yml`:

```yaml
convention:
  changelog: bullet
```

## Outcome

`ProjectMetadataResolver` will use your convention when extracting the changelog for a version.

## Variants

- For README conventions, implement `ReadmeConvention` instead.

## See also

- [Conventions Reference](../reference/conventions.md)
- [Conventions Explanation](../explanation/conventions.md)
