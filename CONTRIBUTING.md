# Contributing to Terracotta

Thank you for your interest in contributing to Terracotta! To keep the project clean, robust, and maintainable, please follow these guidelines.

## VCS

This project uses [Git](https://git-scm.com/) 2.54.0 or later for version control.

### Cloning

Fork the repository on GitHub, then clone your fork locally:

```bash
git clone https://github.com/<your-username>/terracotta.git
cd terracotta
```

Add the upstream remote so you can pull in future changes:

```bash
git remote add upstream https://github.com/beduality/terracotta.git
```

### Commits

This project follows [Conventional Commits](https://www.conventionalcommits.org/) and [Semantic Versioning](https://semver.org/). Scopes map to the module being changed: `core`, `modrinth`, `cli`, `github`.

The commit type determines how the version bumps: `fix` → patch, `feat` → minor, any `BREAKING CHANGE` footer or `!` suffix → major.

### Pull Requests

1. Fork the repository and create your branch from `main`.
2. Keep the PR focused — one logical change per PR makes review and revert easier.
3. Push to your fork and open a Pull Request targeting `main`.
4. Ensure the build, tests, and Spotless check all pass on your branch before requesting review.
5. Fill in the PR description with a summary of what changed and why. If the change affects users, operators, or integrators, describe the impact.

## Project Management

Project planning is tracked in plain Markdown files under `project/`, version-controlled alongside the code. There are no external issue trackers or project boards.

### TODO

`project/TODO.md` holds concrete, actionable tasks that are ready to be picked up. If you want to work on something, check here first. When picking up a task, remove it from the file in the same commit that introduces the work.

### Backlog

`project/BACKLOG.md` holds ideas and tasks that are not yet ready to act on — things that need more thought, depend on other work, or are low priority. Items graduate to `TODO.md` once they are well-defined and ready.

### Proposals

`project/proposals/*.md` is where larger changes are proposed and discussed before any implementation begins. Each proposal is a standalone Markdown file covering the problem, the proposed solution, alternatives considered, and open questions.

To propose a change:

1. Create a file under `project/proposals/` with a descriptive name, e.g. `project/proposals/yaml-schema-support.md`.
2. Open a Pull Request with just the proposal file — no implementation yet.
3. Iterate on the proposal based on review feedback until it is accepted or rejected.

## Architectural Guidelines

This project is structured as a **Multi-Module Gradle project** under the `modules/` directory to separate platform-agnostic business logic from specific registry providers and CLI frontends:

1. **Domain Isolation (`terracotta-core`)**:
   - Contains all canonical models, provider interfaces, and diff engine logic.
   - Depends only on standard Kotlin/Java APIs. Absolutely no network or framework dependencies.
2. **Registry Providers (`terracotta-provider-modrinth`)**:
   - Implements registry-specific behaviors.
   - Depends on Jackson (JSON/YAML) and OkHttp.
3. **CLI Wrapper (`terracotta-cli`)**:
   - Handles the Picocli configuration and standard log outputs.
4. **Infrastructure Management (`terracotta-github`)**:
   - Manages the GitHub repository configuration, metadata, and repository secrets using Pulumi Java/Kotlin.

## Development Workflow

1. **Java Version**: Ensure you are using JDK 21. Since default system default might be Java 26+, prefix your Gradle commands with:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk
   ```
2. **Build and Test**:
   - Run tests:
     ```bash
     JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
     ```
   - Compile native binary:
     ```bash
     JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew nativeCompile
     ```
3. **Code Style**:
   - Spotless handles formatting. Verify formatting before pushing:
     ```bash
     JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessCheck
     ```

## Toolchain

### Kotlin / JVM

The main codebase is written in **Kotlin 2.0.0** targeting JVM 17, compiled under **JDK 21**.

| Concern | Tool | How to run |
|---------|------|------------|
| Dependency management | [Gradle](https://gradle.org/) with version catalog (`gradle/libs.versions.toml`) | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build` |
| Formatting & linting | [ktlint](https://pinterest.github.io/ktlint/) via [Spotless](https://github.com/diffplug/spotless) 6.25.0 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessCheck` |
| Auto-fix formatting | ktlint via Spotless | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessApply` |
| API docs | [Dokka](https://github.com/Kotlin/dokka) 1.9.20 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew dokkaHtml` |
| Testing | JUnit 5.10.2 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test` |

Kotlin does not have a separate type-checker step — type errors surface during compilation (`./gradlew build`).

Spotless must pass before a PR can be merged. Run `spotlessApply` to auto-fix formatting rather than fixing it by hand.

### Python

Python is used for the release process automation and documentation tooling. The minimum required version is **Python 3.13**. Dependencies are managed with [uv](https://docs.astral.sh/uv/).

| Concern | Tool |
|---------|------|
| Dependency management | [uv](https://docs.astral.sh/uv/) |
| Testing | [pytest](https://pytest.org/) |

There is no enforced formatter or linter for Python scripts at this time. Keep scripts readable and consistent with the existing style.

## Tests

Tests are a required part of any contribution that changes behavior. This project has two test tiers with different scopes and prerequisites.

### Unit & Integration Tests

These run as part of the standard build and cover the core business logic, provider implementations, and CLI wiring. They do not require network access or external credentials.

Run them with:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
```

When contributing:

- Place tests alongside the module they exercise (e.g., tests for `terracotta-core` live in `modules/terracotta-core/src/test/`).
- Prefer unit tests for pure logic in `terracotta-core`. Integration tests that wire multiple components together belong in the relevant provider or CLI module.
- Tests must pass on CI before a Pull Request can be merged.

### Smoke Tests

Smoke tests exercise the **full compiled CLI binary** against the live Modrinth API. They verify end-to-end behavior that unit and integration tests cannot cover, such as real network responses, authentication flows, and actual command output.

Because they depend on a live API and a compiled binary, they are intentionally **excluded from the default `./gradlew test` run** and must be triggered explicitly.

#### Prerequisites

- Set the `MODRINTH_TOKEN` environment variable to a valid Modrinth API token.
- Build the CLI distribution first so the binary is available:
  ```bash
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :terracotta-cli:installDist
  ```

#### Running

```bash
MODRINTH_TOKEN=<your-token> JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :terracotta-cli:smokeTest
```

The suite will be skipped automatically (not failed) if the CLI binary is not found, so a missing `installDist` step produces a skip rather than a misleading failure.

> Run smoke tests before submitting a PR that touches provider logic, CLI commands, or anything that affects the Modrinth API integration.

## Documentation

The documentation site is built with [MkDocs](https://www.mkdocs.org/) and the [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) theme. Python dependencies are managed with [uv](https://docs.astral.sh/uv/). Versioning is handled by [mike](https://github.com/jimporter/mike).

Content is organized following the [Diátaxis](https://diataxis.fr/) framework. Every page must belong to exactly one of the four types — tutorial, how-to guide, reference, or explanation — and must stay strictly within that type's purpose. Mixing types in a single page is not acceptable. Read [`guidelines/diataxis.md`](guidelines/diataxis.md) before writing or restructuring any documentation.

### Prerequisites

- [uv](https://docs.astral.sh/uv/getting-started/installation/) installed.

### Local Preview

Since the site uses `mike` for versioning, previewing requires deploying at least one version locally first:

1. Deploy the docs under a local version alias:
   ```bash
   uv run mike deploy -t "Unreleased" unreleased
   uv run mike set-default unreleased
   ```

2. Start the local server:
   ```bash
   uv run mike serve
   ```

This serves the site at `http://localhost:8000` with version switching working as it does in production.

Alternatively, for a quick single-version preview without committing to the `gh-pages` branch:

```bash
uv run mkdocs serve
```

This starts a live-reloading server at `http://localhost:8000` but without multi-version support.

### Structure

```text
docs/
├── index.md                        # Landing page
├── content/
│   ├── tutorials/
│   │   └── getting-started.md
│   ├── how-to-guides/
│   │   ├── ci-cd-setup.md
│   │   └── custom-provider.md
│   ├── reference/
│   │   ├── config-spec.md
│   │   ├── cli.md
│   │   └── api.md
│   └── explanation/
│       └── architecture.md
└── hooks/                          # MkDocs build hooks (copy_kdoc.py, copy_changes.py)
mkdocs.yml                          # Site configuration & navigation
pyproject.toml                      # Python dependencies (uv)
```

### Adding a Page

1. Create a `.md` file under `docs/content/` in the appropriate category.
2. Add the page to the `nav` section in `mkdocs.yml`.
3. Preview with `uv run mkdocs serve`.

### Rebuilding All Versions

If you change templates, overrides, hooks, or `mkdocs.yml` and want to apply them across all historical versions, run:

```bash
uv run python scripts/redeploy_all_docs.py
```

This script checks out each release tag, applies the latest config and overrides from `main`, and redeploys all versions to the local `gh-pages` branch using `mike`.

### Deployment

Documentation is automatically deployed to GitHub Pages on every push to `main` via the `.github/workflows/deploy-docs.yml` workflow. No manual deployment is needed.

## Release Process

We use an automated release script to handle bumping versions, updating the changelog, running dry-run verification, tagging, and pushing, with built-in rollback capabilities.

### Versioning

Releases follow [Semantic Versioning](https://semver.org/). The release script inspects conventional commits since the last tag and suggests the next version automatically:

- `fix` commits → patch bump
- `feat` commits → minor bump
- Any commit with a `BREAKING CHANGE` footer or `!` suffix → major bump

In wizard mode the script displays the detected bump and suggested version as the default choice. You can accept it or override with `patch`, `minor`, `major`, or a custom version string.

### Changelog

`CHANGELOG.md` is maintained manually and deliberately — changelog entries are not generated from commit messages. Commits are often too low-level and implementation-focused to be useful to users, operators, or integrators.

Before releasing, add all user-facing changes under the `## [Unreleased]` section following the guidelines in [`guidelines/changelog.md`](guidelines/changelog.md). The release script then replaces that header with the new version and today's date automatically.

Do not remove the `## [Unreleased]` header — the script depends on it.

### Running the Release Script

```bash
uv run scripts/release.py
```

To see all available options and commands (including rollback):

```bash
uv run scripts/release.py --help
```

### Distribution & Publishing

Once the release script creates and pushes the version tag, the `.github/workflows/release.yml` workflow automatically triggers and handles distribution:

#### CLI Binaries (GitHub Releases)

- Native binaries are built using GraalVM for:
  - Linux (x86_64): `terracotta-linux-amd64`
  - macOS (universal): `terracotta-macos-universal`
  - Windows (x86_64): `terracotta-windows-amd64.exe`
- Binaries are uploaded as artifacts to the GitHub Release page
- Users download these directly from the [Releases](https://github.com/beduality/terracotta/releases) page

#### Maven Central Publishing

The following modules are published to Maven Central with GPG signing:

- **terracotta-core**: `io.github.beduality:terracotta-core`
- **terracotta-provider-modrinth**: `io.github.beduality:terracotta-provider-modrinth`

The workflow uses the [Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin) to automatically close and release the staging repository to Maven Central.

The workflow uses the following secrets (configured in GitHub repository settings):

- `OSSRH_USERNAME`: Sonatype OSSRH username
- `OSSRH_PASSWORD`: Sonatype OSSRH password/token
- `SIGNING_KEY`: GPG private key (ASCII-armored)
- `SIGNING_PASSWORD`: GPG key passphrase

The entire Maven Central publishing process is automated — no manual intervention is required.
