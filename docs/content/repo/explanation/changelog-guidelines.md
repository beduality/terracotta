# Changelog Guidelines

Terracotta keeps a human-readable changelog because commit history is not a release note. The changelog answers one question for consumers: **what changed that affects how the system is used, integrated, run, or depended on — and why it matters.**

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

## How entries are grouped

Entries are grouped by change category and then by module.

### Categories

Use [Keep a Changelog](https://keepachangelog.com/) categories:

- **Added** — new capabilities
- **Changed** — behavior modifications
- **Fixed** — bug fixes
- **Deprecated** — features scheduled for removal
- **Removed** — deleted features
- **Security** — security-related fixes

### Modules

Use the module that contains the changed code:

- **Docs** — documentation, guides, release notes, and the public site
- **Repo** — repository tooling, CI/CD, release scripts, and conventions
- **Core** — `terracotta-core` module
- **Gradle Plugin** — `terracotta-gradle-plugin` module
- **Modrinth** — `terracotta-provider-modrinth` module
- **Hangar** — `terracotta-provider-hangar` module

If a change spans modules, either split it into scoped entries or place it under the most affected module.

## Style principles

- Start each entry with a **past-tense verb** (`Added`, `Fixed`, `Updated`, `Removed`, `Changed`).
- Be specific and concrete, not vague.
- Focus on impact, not implementation.
- Inline the reason with `so`, `because`, or similar instead of using a separate `**Why**:` line.
- Use bold `**Module**` headings under each category; do not use `####` headings for modules.
- Mark breaking changes explicitly with `**Breaking**:` and migration steps.

For the mechanical writing rules and examples, see [How to Write Changelog Entries](../how-to-guides/writing-changelog.md).
