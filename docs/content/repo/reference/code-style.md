# Code Style Reference

This reference documents Kotlin and Python code style for the Terracotta codebase.

## Kotlin

### Basics

- **Language Version**: Kotlin 2.3.21
- **Target JVM**: 17
- **Compiler**: JDK 21

### Formatting

Spotless runs ktlint. Auto-fix with:

```bash
./gradlew spotlessApply
```

CI requires:

```bash
./gradlew spotlessCheck
```

### Formatting rules

| Rule | Value |
|---|---|
| Wildcard imports | Not allowed |
| Line length | 120 characters |
| Braces | K&R style |
| Indentation | 4 spaces, no tabs |

### Naming

| Kind | Convention |
|---|---|
| Classes | `PascalCase` |
| Functions | `camelCase` |
| Constants | `UPPER_SNAKE_CASE` |
| Types | `PascalCase` |

### Serialization annotations

- Use `@Serializable` from Kotlinx Serialization for data models.
- Use `@SerialName` for explicit JSON property names.

## Python

No formatter is enforced for scripts. Keep them consistent with the existing style:

- Follow PEP 8.
- Use descriptive names.
- Include type hints.
- Run scripts with `uv run`.

## Related references

- [Commit Conventions](commit-conventions.md): Conventional Commits format and scopes.
- [Documentation Style](documentation-style.md): How to write docs that match the site conventions.
