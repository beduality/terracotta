# Contributing to Terracotta

Thank you for your interest in contributing to Terracotta! To keep the project clean, robust, and maintainable, please follow these guidelines.

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

---

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

## Smoke Tests

Smoke tests exercise the full CLI binary against the **live Modrinth API** and are intentionally excluded from the default `./gradlew test` run. They must be triggered explicitly.

### Prerequisites

- Set the `MODRINTH_TOKEN` environment variable to a valid Modrinth API token.
- Build the CLI distribution first:
  ```bash
  JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :terracotta-cli:installDist
  ```

### Running

```bash
MODRINTH_TOKEN=<your-token> JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :terracotta-cli:smokeTest
```

The suite will be skipped automatically (not failed) if the CLI binary is not found.

---

## Documentation

The documentation site is built with [MkDocs](https://www.mkdocs.org/) and the [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) theme. Python dependencies are managed with [uv](https://docs.astral.sh/uv/). Versioning is handled by [mike](https://github.com/jimporter/mike).

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

---

## Submitting Pull Requests

1. Fork the repository and create your branch from `main`.
2. Commit your changes with clear, descriptive commit messages.
3. Push to your fork and submit a Pull Request targeting `main`.
4. Ensure the build and tests pass successfully on your branch.
