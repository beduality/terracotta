# Docs Changelog

Tracks changes to the documentation site (structure, pages, style, navigation).
Promoted manually per major version, matching the mike versioning scheme.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

Initial documentation site built with MkDocs Material and deployed at `https://beduality.github.io/terracotta/`, with versioned docs via mike.

### Added

- **Documentation site** — MkDocs Material site with Diátaxis-structured content, estimated reading times on every page, a homepage with feature cards and a plan/apply flowchart, a "Navigating the Docs" guide, and a "Last Changes" page driven by a structured deployment manifest.
    - **Build hooks** — Pre-build hooks for copying generated Dokka API docs and the root `LICENSE` into the docs tree.
    - **KDoc** — `@see` links and member-level KDoc on all public APIs across `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`, pointing to the GitHub Pages docs.
- **Quick Start** — Getting Started tutorial for publishing a first release to Modrinth with the Gradle plugin, plus a License page.
- **Integration** — End-to-end guides for wiring Terracotta into real projects: a Modrinth publishing tutorial, how-to guides for adding Modrinth and Hangar to the Gradle plugin, a troubleshooting guide, provider configuration reference, and integration design explanation.
- **Modules** — Per-module documentation with Diátaxis sections (tutorials, how-to guides, reference, explanation) for each component:
    - **Core module** — Tutorials (installation, custom loaders, custom metadata detectors, custom providers), how-to guides (loading config, resolving metadata, computing diffs, adding loaders and conventions, normalizing game versions), reference pages (models, loaders, operations, config schema, metadata resolution, conventions, version conventions, provider interfaces, Dokka API docs), and explanation pages (architecture, provider logic, metadata resolution, diff engine, state management, loader hierarchy, conventions).
    - **Gradle Plugin module** — Installation and Getting Started tutorials, Kotlin DSL configuration guide, tasks reference, Dokka API docs, and explanation pages (architecture, DSL design, task lifecycle).
    - **State Filesystem module** — README, filesystem backend configuration guide, state filesystem reference, Dokka API docs, and design explanation covering why YAML and file-backed persistence are the defaults.
    - **Modrinth Provider module** — Using Modrinth tutorial, Dokka API docs, and explanation pages (architecture, version mapping).
    - **Hangar Provider module** — Using Hangar tutorial, Dokka API docs, and explanation pages (architecture, loader mapping).
    - **Repo** — Contributor documentation covering building, testing, contributing, releasing, smoke testing, changelog writing, code style, commit conventions, CI/CD, branch strategy, file structure, tech stack, and project management.
