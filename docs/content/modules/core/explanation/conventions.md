# Conventions

Conventions separate file-format knowledge from metadata detectors. A convention answers one question for one file type.

## Why conventions are pluggable

README and changelog formats vary across projects. Some teams use Keep a Changelog, others use custom formats. Hardcoding every format inside detectors would make core grow endlessly and would prevent users from adopting their own formats.

## What a convention does

A convention turns raw file content into semantic values:

- A `ReadmeConvention` extracts description and summary from `README.md`.
- A `ChangelogConvention` extracts release notes for a specific version from `CHANGELOG.md`.

## Why registration is explicit

`ProjectFileConventionRegistry.load()` must be called before resolution. This gives callers control over when conventions are initialized and lets tests register extra conventions deterministically.

## Built-in conventions

| ID | File | Purpose |
|----|------|---------|
| `terracotta` | `README.md` | Extract description and summary. |
| `keep-a-changelog` | `CHANGELOG.md` | Extract version sections from Keep a Changelog format. |

## Adding a convention

Conventions are discovered through Java's `ServiceLoader` or registered at runtime. This mirrors the provider SPI and keeps the extension mechanism consistent across core.

## See also

- [Conventions Reference](../reference/conventions.md)
- [Add a Project-File Convention](../how-to-guides/add-a-new-project-file-convention.md)
