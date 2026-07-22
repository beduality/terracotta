# Writing Changelog Entries

This guide covers the mechanical rules for adding a changelog entry.

## Where to add an entry

- **Module changes** (Core, Gradle Plugin, Modrinth, Hangar, State Filesystem): Add entries under `## [Unreleased]` in the corresponding `modules/<module>/CHANGELOG.md`.
- **Documentation changes** (page additions, reorganizations, style, navigation): Add entries under `## [Unreleased]` in `docs/CHANGELOG.md`.
- **Repo-wide changes** (CI/CD, tooling, conventions): Add entries under the current date heading in the root `CHANGELOG.md`.

## Release summary

Every release section, including `## [Unreleased]`, must start with a summary paragraph before the first `###` category heading. The summary should capture the release's themes and impact in two to four sentences.

```md
## [Unreleased]

Added project link management and full Gradle DSL parity for icons and links, while unifying how Modrinth and Hangar providers map remote URLs.

### Added
```

Summaries are required. If the release only contains one small fix, a single sentence is still required.

## Format

```md
### Category

- Short description of what changed so the consumer-visible reason is clear.
```

## Rules

- **Start every release section with a summary.** The summary goes immediately under the `## [Version]` heading, before the first `###` category. Keep it to two to four sentences that state the release's themes and impact.
- **Write `[Unreleased]` summaries directly.** Start with a past-tense verb (e.g. "Added...", "Fixed...", "Narrowed..."), not with a meta-introduction such as "This release..." or "This unreleased set of changes...". The release script copies the `[Unreleased]` body into the new version section verbatim, so meta-introductions become redundant or incorrect after release.
- **Start each entry with a past-tense verb.** Use `Added`, `Fixed`, `Updated`, `Removed`, `Changed`, `Configured`, etc.
- **Be specific.** Say what changed in concrete terms, not "improved" or "refactored".
- **Focus on impact, not implementation.** A consumer should understand the change without reading the code.
- **No implementation details.** Do not mention imports, internal classes, method names, file renames, or how code was decoupled unless that detail is itself part of the public API or contract.
- **Derive entries from the git diff since the last release tag.** Review only what is new relative to that tag and describe the observable change, not the diff lines.
- **Inline the reason.** Use `so`, `because`, or similar to make the consumer-visible benefit part of the entry instead of a separate `**Why**:` line.
- **Group related changes.** Put multiple bullets under the same category when they belong together.
- **Describe the final state, not the process.** If a feature was added and then fixed before release, write one entry for the working result. Do not list both the addition and the fix — consumers never saw the intermediate state.
- **Keep entries concise.** Put detail, caveats, or migration steps in `**Breaking**:` or `**Migration**:` lines when needed.

## Examples

### Good

```md
### Fixed

- Fixed publishing of `-javadoc.jar` artifacts so they include generated API documentation instead of empty JARs, satisfying Maven Central requirements.
```

### Bad

```md
### Fixed

- Refactored `JavadocTask` to use `DokkaJavadoc`.
```

This describes implementation, not impact.

```md
### Changed

- The Gradle plugin no longer imports `FileSystemStateSource` directly. The default state filename is now a local plugin constant, and `StateSourceResolver` collects discovered factory IDs.
```

This names internal classes, imports, and implementation mechanics. A consumer-facing version would describe the clearer missing-backend error and the reduced coupling to a specific backend.

## Breaking changes

Mark breaking changes explicitly:

```md
### Changed

- `TerracottaVersion` fields are now required.
  - **Breaking**: Versions without required fields will fail validation.
  - **Migration**: Add all required fields to your version definitions.
```

## Before submitting

Ask:

- Does this affect usage, integration, runtime, or deployment?
- Can a consumer observe it?
- Does it change behavior or contracts?
- Does it require action from users or operators?

If all answers are "no", do not add an entry.

For the reasoning behind these rules, see [Changelog Guidelines](../reference/changelog-guidelines.md).
