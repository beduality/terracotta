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
- Added a pre-build hook that copies `LICENSE` into the docs site so the license page is reachable without leaving the generated site.
- Added a License page to the Quick Start navigation.

#### Quick Start section
- Added a Getting Started tutorial for publishing a first release to Modrinth using the Gradle plugin.

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
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-core`, pointing to the GitHub Pages docs.

#### Gradle Plugin module section
- Added a Gradle Plugin module README introducing the `terracotta` DSL, tasks, and CI/CD integration.
- Added tutorials: Installation, Getting Started.
- Added a how-to guide for Kotlin DSL Configuration.
- Added reference pages: Tasks Reference, API Docs (Dokka).
- Added explanation pages: Architecture, DSL Design, Task Lifecycle.
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-gradle-plugin`, pointing to the GitHub Pages docs.

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
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-provider-modrinth`, pointing to the GitHub Pages docs.

#### Hangar Provider module section
- Added a Hangar Provider module README.
- Added a "Using Hangar" tutorial.
- Added reference pages: API Docs (Dokka).
- Added explanation pages: Architecture, Loader Mapping.
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-provider-hangar`, pointing to the GitHub Pages docs.

#### Repo section
- Added a Repo README for contributors, maintainers, and release managers.
- Added tutorials: Quick Start (building locally), Navigating the Codebase, First Contribution.
- Added how-to guides: Contributing, Building, Testing, Documentation, Releasing, Smoke Testing a Release, Writing Changelog Entries.
- Added reference pages: Tech Stack, File Structure, Configuration, Scripts, CI/CD, Branches, Project Files, Code Style, Commit Conventions, Documentation Style, Changelog Guidelines.
- Added explanation pages: Project Management, Repo Architecture, Branch Strategy, Release Design, Changelog Design.

#### Content organization
- Reorganized user documentation under `docs/content/modules/` and `docs/content/integration/` with Diátaxis sections for Core, Gradle Plugin, Modrinth Provider, and Hangar Provider.
- Simplified homepage, integration tutorial, and getting-started docs to feature Modrinth as the default single provider, moving Hangar coverage to a follow-up how-to guide.
- Decoupled Core documentation from Gradle plugin and state-management implementation details. Core docs now describe the generic state SPI and metadata resolution, and link to the Gradle plugin and `terracotta-state-filesystem` docs for frontend-specific examples.
- Removed Gradle DSL and build-tool assumptions from core KDoc (`StateSourceConfig`, `StateSourceFactory`, `TerracottaConfig`, `ProjectMetadataSource`).
- Tightened module focus in the `terracotta-state-filesystem` reference and the Gradle plugin Kotlin DSL guide so each page owns its own responsibilities and links across modules.
- Expanded the `terracotta-state-filesystem` documentation with a rewritten README, an expanded reference page, a how-to guide for replacing the filesystem backend, and an explanation of why YAML and file-backed persistence are the defaults.
- Generalized the Modrinth provider tutorial so registry-specific docs no longer depend on Gradle DSL syntax.
- Removed the separate `docs/content/sdk/` section and folded its remaining pages into module docs: installation moved to Core tutorials, custom-provider content merged into Core tutorials, provider API reference merged into Core reference, architecture explanation merged into Core explanation, and the Modrinth quick-start merged into the Modrinth provider tutorial.
- Enforced strict Diátaxis discipline in `docs/content/repo/`: moved architecture explanation to Core explanation, rewrote project management page to remove procedural steps, moved changelog guidelines to reference, and added a clear target-audience statement to the repo README.
- Simplified module READMEs by removing redundant inline Diátaxis explanations and linking readers to the shared "Navigating the Docs" page.
- Moved the Changes link from the custom docs header into the main navigation so it works on mobile.
- Reorganized changelog docs into a succinct explanation and a practical how-to guide.
- Moved project introduction, installation, and usage content from `README.md` into `docs/index.md` so the docs site is the single source of truth for presentation content.
- Added a new `Config` docs category with `Overview` and `Schema` pages so users can find the file-format reference independently of Gradle-plugin guides.
- Added the smoke-testing release guide in plain language and added guidance on saving pytest JSON reports as metrics.
- Added multi-module API reference links to the docs navigation.
- Added a "Navigating the Docs" page to the Quick Start section so readers can understand the Diátaxis structure and find the right section.
- Added a complete set of repo documentation: reference pages for scripts, CI/CD, branches, project files, commit conventions, and documentation style; explanation pages for repo architecture, branch strategy, release design, and changelog design; and a first-contribution tutorial.
- Added integration documentation covering multi-provider publishing, provider configuration, integration design, and troubleshooting.
- Added Dokka documentation generation to the `deploy-docs.yml` workflow so API reference docs are generated automatically on every docs deploy.
- Configured versioned docs to build from the release tag, so the docs site updates as soon as a release goes out.

