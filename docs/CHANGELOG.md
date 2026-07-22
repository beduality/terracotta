# Docs Changelog

Tracks changes to the documentation site (structure, pages, style, navigation).
Promoted manually per major version, matching the mike versioning scheme.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

Initial documentation site covering all modules, integration guides, and repository contribution docs, organized using the [Diátaxis](https://diataxis.fr/) framework.

### Added

#### Site infrastructure
- Built an MkDocs Material documentation site deployed at `https://beduality.github.io/terracotta/` with versioned docs via mike.
- Added a homepage (`index.md`) with feature cards, a Mermaid flowchart of the plan/apply pipeline, stable identity key info, and example plan output in accordions.
- Added a "Navigating the Docs" page explaining the Diátaxis structure (tutorials, how-to guides, reference, explanation) and top-level sections.
- Added a "Last Changes" page driven by a structured `deployments.json` manifest with filtering, search, and module-based browsing.
- Added estimated reading time to every documentation page via `mkdocs-macros-plugin` and a custom Material content override.
- Added build hooks for copying generated Dokka API docs into the site and copying the root `LICENSE` into the docs tree.

#### Quick Start section
- Added a Getting Started tutorial for publishing a first release to Modrinth using the Gradle plugin.
- Added a License page.

#### Integration section
- Added an Integration Overview page listing available providers and the state filesystem backend.
- Added a "Publishing to Modrinth" tutorial covering end-to-end project setup, configuration, and publishing.
- Added how-to guides for adding Modrinth and Hangar to the Gradle plugin.
- Added a Troubleshooting guide for common provider integration issues.
- Added a Provider Configuration reference page.
- Added an Integration Design explanation page.

#### Core module section
- Added a Core module README introducing the domain library, provider interfaces, and diff engine.
- Added tutorials: Installing Terracotta as a Library, Implement a Custom Loader, Implement a Custom Metadata Detector, Implement a Custom Provider.
- Added how-to guides: Load a `terracotta.yml` File, Resolve Project Metadata, Compute a Diff, Add a New Loader, Add a Project-File Convention, Normalize Game Versions.
- Added reference pages: Models, Loaders, Operations, Config Schema, Metadata Resolution, Conventions, Version Conventions, Provider Interfaces, API Docs (Dokka).
- Added explanation pages: Architecture, Provider Logic, Metadata Resolution, Diff Engine, State Management, Loader Hierarchy, Conventions.

#### Gradle Plugin module section
- Added a Gradle Plugin module README introducing the `terracotta` DSL, tasks, and CI/CD integration.
- Added tutorials: Installation, Getting Started.
- Added a how-to guide for Kotlin DSL Configuration.
- Added reference pages: Tasks Reference, API Docs (Dokka).
- Added explanation pages: Architecture, DSL Design, Task Lifecycle.

#### State Filesystem module section
- Added a State Filesystem module README introducing the file-backed state backend.
- Added a how-to guide for Configuring the Filesystem Backend.
- Added reference pages: State Filesystem, API Docs (Dokka).
- Added an explanation page on State Filesystem Design covering why YAML and file-backed persistence are the defaults.

#### Modrinth Provider module section
- Added a Modrinth Provider module README.
- Added a "Using Modrinth" tutorial.
- Added reference pages: API Docs (Dokka).
- Added explanation pages: Architecture, Version Mapping.

#### Hangar Provider module section
- Added a Hangar Provider module README.
- Added a "Using Hangar" tutorial.
- Added reference pages: API Docs (Dokka).
- Added explanation pages: Architecture, Loader Mapping.

#### Repo section
- Added a Repo README for contributors, maintainers, and release managers.
- Added tutorials: Quick Start (building locally), Navigating the Codebase, First Contribution.
- Added how-to guides: Contributing, Building, Testing, Documentation, Releasing, Smoke Testing a Release, Writing Changelog Entries.
- Added reference pages: Tech Stack, File Structure, Configuration, Scripts, CI/CD, Branches, Project Files, Code Style, Commit Conventions, Documentation Style, Changelog Guidelines.
- Added explanation pages: Project Management, Repo Architecture, Branch Strategy, Release Design, Changelog Design.

### Changed

- Reworked the docs homepage (`index.md`) to be more succinct: replaced the dense numbered workflow description with a Mermaid diagram and folded the gallery note and example output into accordions.
- Decoupled Core documentation from Gradle plugin and state-management implementation details. Core docs now describe the generic state SPI and metadata resolution, and link to the Gradle plugin and `terracotta-state-filesystem` docs for frontend-specific examples.
- Removed Gradle DSL and build-tool assumptions from core KDoc (`StateSourceConfig`, `StateSourceFactory`, `TerracottaConfig`, `ProjectMetadataSource`).
- Tightened module focus in the `terracotta-state-filesystem` reference and the Gradle plugin Kotlin DSL guide so each page owns its own responsibilities and links across modules.
- Expanded the `terracotta-state-filesystem` documentation with a rewritten README, an expanded reference page, a how-to guide for replacing the filesystem backend, and an explanation of why YAML and file-backed persistence are the defaults.
- Generalized the Modrinth provider tutorial so registry-specific docs no longer depend on Gradle DSL syntax.
- Simplified homepage, integration tutorial, and getting-started docs to feature Modrinth as the default single provider, moving Hangar coverage to a follow-up how-to guide.
- Reorganized user documentation under `docs/content/modules/` and `docs/content/integration/` with Diátaxis sections for Core, Gradle Plugin, Modrinth Provider, and Hangar Provider; folded `docs/content/config/` into the Core reference.
- Removed the separate `docs/content/sdk/` section and folded its remaining pages into module docs: installation moved to Core tutorials, custom-provider content merged into Core tutorials, provider API reference merged into Core reference, architecture explanation merged into Core explanation, and the Modrinth quick-start merged into the Modrinth provider tutorial.
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`, pointing to the GitHub Pages docs.
- Enforced strict Diátaxis discipline in `docs/content/repo/`: moved architecture explanation to Core explanation, rewrote project management page to remove procedural steps, moved changelog guidelines to reference, and added a clear target-audience statement to the repo README.
- Simplified module READMEs by removing redundant inline Diátaxis explanations and linking readers to the shared "Navigating the Docs" page.
- Moved the Changes link from the custom docs header into the main navigation so it works on mobile.
- Reorganized changelog docs into a succinct explanation and a practical how-to guide.
- Moved project introduction, installation, and usage content from `README.md` into `docs/index.md` so the docs site is the single source of truth for presentation content.

### Fixed

- Fixed broken MkDocs Material content tab syntax in authentication sections of the Gradle plugin and integration how-to guides.
- Removed obsolete per-provider plan/apply task names from the docs homepage quick reference.
- Updated Gradle plugin and provider tutorials to explicitly install the `terracotta-state-filesystem` backend instead of assuming it is bundled.
- Fixed the docs License page by copying the root `LICENSE` to `docs/LICENSE.md`, adding it to the Quick Start navigation, and updating the index link so the page is reachable and rendered correctly.
- Fixed the pre-build hook destination so `LICENSE` is copied to `docs/LICENSE` instead of `docs/LICENSE.md`.
- Removed the CI/CD setup with GitHub Actions guide from `docs/content/integration/` because the workflow is not currently supported.
- Cleaned up `docs/content/repo/` by removing redundant pages, trimming the Diátaxis framework explanation, updating outdated navigation references, and removing remaining `SDK` terminology.
- Removed the `CI/CD Deployment` how-to guide from the docs navigation because the current stateless design does not support the documented workflow.
- Removed the stale `docs/overrides` reference from `mkdocs.yml` so docs deployments no longer fail with a missing custom_dir path.
- Fixed docs deployments so the live site reliably matches the latest generated release, preventing stale or mismatched content.
- Configured versioned docs to build from the release tag, so the docs site updates as soon as a release goes out.
- Fixed docs deployment so routine pushes no longer reset the default site version from `latest` to `unreleased`.
- Added multi-module API reference links to the docs navigation.
- Fixed broken internal links and replaced full URLs with relative links where appropriate.

