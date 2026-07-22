# Docs Changelog

Tracks changes to the documentation site (structure, pages, style, navigation).
Promoted manually per major version, matching the mike versioning scheme.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

Built the initial documentation site with MkDocs Material and deployed at `https://beduality.github.io/terracotta/`, with versioned docs via mike.

### Added

- Added documentation site built with MkDocs Material, with Diátaxis-structured content, estimated reading times on every page, a homepage with feature cards and a plan/apply flowchart, a "Navigating the Docs" guide, and a "Last Changes" page driven by a structured deployment manifest.
    - Added pre-build hooks for copying generated Dokka API docs and the root `LICENSE` into the docs tree.
    - Added `@see` links and member-level KDoc on all public APIs across `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`, pointing to the GitHub Pages docs.
- Added Getting Started tutorial for publishing a first release to Modrinth with the Gradle plugin, plus a License page.
- Added integration guides for wiring Terracotta into real projects: a Modrinth publishing tutorial, how-to guides for adding Modrinth and Hangar to the Gradle plugin, a troubleshooting guide, provider configuration reference, and integration design explanation.
- Added per-module documentation with Diátaxis sections (tutorials, how-to guides, reference, explanation) for each component:
    - Added Core module docs: tutorials (installation, custom loaders, custom metadata detectors, custom providers), how-to guides (loading config, resolving metadata, computing diffs, adding loaders and conventions, normalizing game versions), reference pages (models, loaders, operations, config schema, metadata resolution, conventions, version conventions, provider interfaces, Dokka API docs), and explanation pages (architecture, provider logic, metadata resolution, diff engine, state management, loader hierarchy, conventions).
    - Added Gradle Plugin module docs: installation and Getting Started tutorials, Kotlin DSL configuration guide, tasks reference, Dokka API docs, and explanation pages (architecture, DSL design, task lifecycle).
    - Added State Filesystem module docs: README, filesystem backend configuration guide, state filesystem reference, Dokka API docs, and design explanation covering why YAML and file-backed persistence are the defaults.
    - Added Modrinth Provider module docs: Using Modrinth tutorial, Dokka API docs, and explanation pages (architecture, version mapping).
    - Added Hangar Provider module docs: Using Hangar tutorial, Dokka API docs, and explanation pages (architecture, loader mapping).
    - Added Repo contributor documentation covering building, testing, contributing, releasing, smoke testing, changelog writing, code style, commit conventions, CI/CD, branch strategy, file structure, tech stack, and project management.

### Changed

- Changed Last Changes page: reordered deployment header (title, version badge, module icons, release tag, date), enabled multiple module badge filters simultaneously, and added support for versionless deployment entries (no version badge shown).
- Updated changelog explanation, guidelines, how-to, and file structure docs to describe the three-tier changelog system (root, docs, per-module) and the repo-wide activity log format with dated sections.
- Added `docs/CHANGELOG.md` to the docs nav and navigating-docs page.
