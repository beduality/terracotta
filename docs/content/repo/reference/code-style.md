# Code Style Reference

This reference documents the code style and formatting requirements for Terracotta.

## Kotlin Style

### Basics

- **Language Version**: Kotlin 2.3.21
- **Target JVM**: 17
- **Compiler**: JDK 21

### Formatting

- **Tool**: ktlint via Spotless 6.25.0
- **Run**: `./gradlew spotlessApply` (auto-fix)
- **Check**: `./gradlew spotlessCheck` (CI requirement)

**Configuration**: Found in `build.gradle.kts` under `spotless` block.

### Conventions

1. **Imports**: No wildcard imports (`import package.*`)
2. **Line Length**: Max 120 characters
3. **Braces**: K&R style
4. **Spacing**: 4 spaces, no tabs
5. **Naming**:
   - Classes: `PascalCase`
   - Functions: `camelCase`
   - Constants: `UPPER_SNAKE_CASE`
   - Types: `PascalCase`

### Annotations

- Use `@Serializable` from Kotlinx Serialization for data models
- Use `@SerialName` for explicit JSON property names

## Commit Style

### Conventional Commits

Terracotta follows [Conventional Commits](https://www.conventionalcommits.org/).

**Format**: `<type>(<scope>): <description>`

**Types**:

| Type | Purpose | Version Bump |
|------|---------|--------------|
| `feat` | New feature | Minor |
| `fix` | Bug fix | Patch |
| `docs` | Documentation | None |
| `refactor` | Code refactoring | None |
| `test` | Test additions/changes | None |
| `chore` | Maintenance | None |

**Scopes**:

| Scope | Module |
|-------|--------|
| `core` | `terracotta-core` |
| `modrinth` | `terracotta-provider-modrinth` |
| `gradle-plugin` | `terracotta-gradle-plugin` |
| `github` | `terracotta-github` |
| `docs` | Documentation |
| `ci` | CI/CD workflows |

### Examples

```
feat(core): add TerracottaVersion.displayName field
fix(modrinth): handle missing project gracefully
refactor(gradle-plugin): simplify provider discovery
docs: fix architecture diagram in README
```

### Breaking Changes

Mark with `!` suffix or `BREAKING CHANGE` footer:

```
feat(core)!: remove deprecated API
```

```
feat(core): new API for version management

BREAKING CHANGE: Old API removed
```

## Pull Request Requirements

See the [Contributing how-to guide](../how-to-guides/contributing.md) for the PR checklist and process.

## Python Style

**No formal formatter enforced**, but scripts should:

- Follow PEP 8 style guide
- Use descriptive names
- Include type hints
- Be consistent with existing code

## Documentation Style

- Follow the [Documentation Framework](../../navigating-docs.md) for content organization
- Each page belongs to exactly one type (Tutorial, How-to, Reference, Explanation)
- Use markdown headers for structure
- Include code examples where helpful
- Link to related content
