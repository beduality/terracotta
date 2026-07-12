# Commit Conventions Reference

Terracotta follows [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

## Format

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

## Types

| Type | Purpose | Version Bump |
|---|---|---|
| `feat` | New feature | Minor |
| `fix` | Bug fix | Patch |
| `docs` | Documentation only | None |
| `refactor` | Code change that neither fixes a bug nor adds a feature | None |
| `test` | Test additions or changes | None |
| `chore` | Maintenance tasks | None |

## Scopes

| Scope | Module or area |
|---|---|
| `core` | `terracotta-core` |
| `modrinth` | `terracotta-provider-modrinth` |
| `hangar` | `terracotta-provider-hangar` |
| `gradle-plugin` | `terracotta-gradle-plugin` |
| `github` | `terracotta-github` |
| `docs` | Documentation |
| `repo` | Repository tooling |
| `ci` | CI/CD workflows |

## Breaking changes

Mark breaking changes with a `!` suffix after the type/scope:

```
feat(core)!: remove deprecated API
```

Or with a `BREAKING CHANGE` footer:

```
feat(core): new API for version management

BREAKING CHANGE: old Version class removed
```

## Examples

```
feat(core): add TerracottaVersion.displayName field
fix(modrinth): handle missing project gracefully
refactor(gradle-plugin): simplify provider discovery
docs(repo): fix architecture diagram in README
```

## Version bump mapping

The release script uses commit history since the last tag to suggest a semver bump:

| Marker | Bump |
|---|---|
| `BREAKING CHANGE` or `!` | Major |
| `feat` | Minor |
| `fix` | Patch |
| No matching commits | Patch (fallback) |
