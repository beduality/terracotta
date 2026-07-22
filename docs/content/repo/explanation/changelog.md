# Changelog Design

Terracotta keeps human-written changelogs instead of generating release notes from Git commits. Each publishable module has its own `CHANGELOG.md` under `modules/<module>/CHANGELOG.md`, a `docs/CHANGELOG.md` tracks documentation site changes, and a root `CHANGELOG.md` serves as an ephemeral repo-wide activity log (CI/CD, tooling, conventions). This page explains why.

## Why not auto-generate from commits?

Commit history is a record of what changed in the codebase, not a story for the people who use the project. Generated changelogs inherit every problem of the underlying commits:

- **Implementation details dominate.** A commit like `refactor(core): extract loader resolution into private function` is accurate for code review but useless to someone deciding whether to upgrade.
- **Messages are written for reviewers, not users.** A reviewer cares about the approach; a user cares about behavior, compatibility, and migration.
- **Noise accumulates.** Formatting fixes, test additions, and internal refactors drown out changes that actually affect consumers.
- **No coherent narrative.** A list of commit subjects does not explain whether a set of changes is safe to adopt or what work an upgrade requires.

Auto-generated notes save time for maintainers but push the cost onto readers. Terracotta prefers the opposite: maintainers spend a little more effort so users, operators, and integrators can scan a release and know immediately what matters.

## What a release section answers

A release section begins with a short summary paragraph that answers:

1. **What is this release about?** — the themes or high-level story.
2. **Why should I care?** — the main benefit, risk, or required action.

After the summary, each entry answers three questions:

1. **What changed?** — the observable behavior, not the code.
2. **Who is affected?** — users of the Gradle plugin, SDK consumers, operators, contributors.
3. **Why does it matter?** — the benefit, fix, risk, or required action.

For example, instead of `feat(modrinth): add retry logic`, the changelog says:

> Fixed transient Modrinth upload failures so publishing no longer fails when the API returns a 500 on the first attempt.

The implementation detail (retry logic) is hidden. The user-visible outcome (publishing is more reliable) is front and center.

## Why Keep a Changelog categories?

A release starts with a summary for the high-level story, then entries are grouped under [Keep a Changelog](https://keepachangelog.com/) categories (`Added`, `Changed`, `Fixed`, etc.). Module-specific entries live in each module's own `CHANGELOG.md`; documentation changes go in `docs/CHANGELOG.md`; the root changelog is an ephemeral activity log that uses dated sections instead of version headers.

- **Severity.** `Fixed` suggests a safe upgrade; `Changed` suggests checking behavior; `Removed` or breaking markers signal required action.
- **Scope.** Each module has its own changelog, so consumers only need to read the changelog for the module they depend on. Documentation changes are tracked separately in `docs/CHANGELOG.md`.

Categories also catch mistakes. If a change is hard to place in `Added`, `Changed`, `Fixed`, `Deprecated`, `Removed`, or `Security`, it may not be user-facing enough to mention.

## Relationship to Conventional Commits

Conventional Commits and the changelog serve different audiences:

- **Commits** are for code review and release automation. They determine the semver bump and help reviewers understand the patch.
- **Changelog entries** are for consumers. They describe impact and upgrade considerations.

The two systems are complementary. Commits feed the release script; the changelog feeds the humans reading the release.

## When to skip an entry

Not every change belongs in the changelog. Skip internal-only work such as:

- Pure refactors with no external effect.
- Formatting or lint-only changes.
- Dependency updates that change nothing observable.
- Test-only changes that do not fix a reported bug.

If a change cannot be described in terms of user, operator, or integrator impact, it does not need a changelog entry.

## Where the rules live

For the mechanical format, grouping rules, and examples, see the [Changelog Guidelines](../reference/changelog-guidelines.md) and [Writing Changelog Entries](../how-to-guides/writing-changelog.md).

## Deployment manifest

The changelog is the source of truth for what changed, but the docs "Changes" page is driven by a structured `deployments.json` manifest. This separation lets the UI provide filtering, search, and module-based browsing that a raw markdown changelog cannot.

### Schema

Each deployment entry has:

- **`version`** — Semver string (e.g. `"0.8.0"`).
- **`createdAt`** — ISO 8601 datetime (e.g. `"2026-07-13T00:00:00Z"`). Rendered in the UI using `Intl.DateTimeFormat` for English international format.
- **`title`** — Short human-readable title, derived from the changelog summary or entered manually.
- **`summary`** — One-to-four sentence summary extracted from the changelog's first paragraph.
- **`modules`** — List of canonical module identifiers touched by this deployment (e.g. `["core", "gradle-plugin", "modrinth"]`).
- **`isRelease`** — `true` for major milestones (minor or major bumps); `false` for routine patch deployments.

### Generation

During each release, `scripts/release.py` calls `scripts/deployments.py` to:

1. Parse the module's changelog section for the new version.
2. Extract the summary (first paragraph before `###` headings).
3. Derive a title from the summary by stripping leading verbs and articles.
4. Use the module's canonical identifier as the `modules` field.
5. Append the entry to `deployments.json`, replacing any existing entry with the same version.

### Releases vs deployments

**Releases** are major milestones (minor or major version bumps). Everything else is a routine **deployment**. The Changes page distinguishes the two with a badge and a "Releases only" filter. The full history is kept in `deployments.json`; the UI shows the last 10 by default with a "Show all" button.
