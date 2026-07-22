# Changelog Guidelines

Terracotta keeps human-readable changelogs because commit history is not a release note. Each publishable module has its own `CHANGELOG.md` under `modules/<module>/CHANGELOG.md` for module-specific changes, a `docs/CHANGELOG.md` for documentation site changes, and a root `CHANGELOG.md` as an ephemeral repo-wide activity log (CI/CD, tooling, conventions). The changelogs answer one question for consumers: **what changed that affects how the system is used, integrated, run, or depended on — and why it matters.**

## Why this matters

A changelog is a trust signal. It tells users, operators, and integrators whether they should upgrade, what behavior to expect, and whether they need to change their own code or configuration. It is not a commit log, implementation diary, or internal refactoring log.

## What belongs in the changelog

Include changes that are observable outside the codebase:

- New features or capabilities
- Behavior changes, including new defaults
- Bug fixes
- Deprecations and removals
- Breaking changes
- Deployment, packaging, or tooling changes that affect consumers
- Security fixes

Do not include:

- Internal refactors with no external effect
- Formatting, lint, or style-only changes
- Dependency updates that change nothing observable
- Commit messages or implementation details
- Class, method, import, or file names that are not part of the public API
- Descriptions of how code was reorganized, inlined, or decoupled unless the reorganization itself creates a consumer-visible change

If the only way to describe a change is by naming internal code, it probably does not belong in the changelog.

## Source of truth: diff since the last release

Changelog entries must be derived from the git diff since the last release tag. Review only the changes that are new relative to that tag; do not copy entries from older releases or describe work already shipped.

When writing an entry:

1. Run `git diff <last-release-tag>..HEAD -- <paths>` for the changed source and documentation files.
2. Identify what a consumer can observe or must do differently.
3. Describe that observable change, not the implementation that produced it.
4. If the diff shows only internal refactoring with no observable effect, skip it.

The changelog is a changes log for consumers, not a development log for contributors.

## Release summary

Every release section must start with a short summary paragraph before the first category heading. The summary states the release's themes and why they matter, in two to four sentences. It is not a list of every change; readers should get the story at a glance and then use the categories for details.

Summaries are required for every release, including `[Unreleased]`.

Summarize directly. Start with the substance of the release (for example, "Adds...", "Fixes...", "Narrows..."), not with a meta-introduction such as "This release..." or "This unreleased set of changes...". The section heading already identifies the release, and the release script promotes the `[Unreleased]` body verbatim into the new version section, so meta-introductions become incorrect or redundant the moment the version is released.

## How entries are grouped

Module-specific entries go in each module's own `CHANGELOG.md`, grouped by change category. Documentation changes go in `docs/CHANGELOG.md`. Repository-wide entries (CI/CD, tooling) go in the root `CHANGELOG.md` under the current date heading.

### Categories

Use [Keep a Changelog](https://keepachangelog.com/) categories:

- **Added** — new capabilities
- **Changed** — behavior modifications
- **Fixed** — bug fixes
- **Deprecated** — features scheduled for removal
- **Removed** — deleted features
- **Security** — security-related fixes

### Where to add entries

- **Module changes** (Core, Gradle Plugin, Modrinth, Hangar, State Filesystem): Add entries under `## [Unreleased]` in the corresponding `modules/<module>/CHANGELOG.md`.
- **Documentation changes** (page additions, reorganizations, style, navigation): Add entries under `## [Unreleased]` in `docs/CHANGELOG.md`.
- **Repo-wide changes** (CI/CD, tooling, conventions): Add entries under the current date heading in the root `CHANGELOG.md`, using `### Category` headings with plain bullet lists.

## Style principles

- Start each release section with a **summary paragraph** that captures the release's themes and impact in two to four sentences.
- **Summarize directly** in the summary paragraph; do not begin with a meta-introduction such as "This release..." or "This unreleased set of changes...". The section heading already identifies the release.
- Start each entry with a **past-tense verb** (`Added`, `Fixed`, `Updated`, `Removed`, `Changed`).
- Be specific and concrete, not vague.
- Focus on impact, not implementation.
- Inline the reason with `so`, `because`, or similar instead of using a separate `**Why**:` line.
- Mark breaking changes explicitly with `**Breaking**:` and migration steps.

For the reasoning behind these rules, see [Changelog Design](../explanation/changelog.md).

For the mechanical writing rules and examples, see [How to Write Changelog Entries](../how-to-guides/writing-changelog.md).
