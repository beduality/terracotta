# File Structure Reference

```
.
├── .agent/                 # Agent rules and guidelines
├── .devin/                 # Devin hooks and configuration
│   ├── hooks/              # Post-tool-use automation hooks
│   └── slash-commands/     # Custom Devin slash commands (e.g. /proposal)
├── .github/                # GitHub Actions workflows and templates
├── .vscode/                # VSCode workspace settings and extensions
├── docs/                   # Documentation (MkDocs)
│   ├── content/            # Content organized by topic (modules, integration, repo)
│   ├── CHANGELOG.md        # Documentation site changelog (promoted per major version)
│   └── hooks/              # Build hooks for copying generated docs
├── gradle/                 # Gradle wrapper and version catalogs
├── modules/                # Gradle modules (each with its own CHANGELOG.md and gradle.properties)
├── project/                # Project management
├── scripts/                # Utility scripts (release, deployment)
├── build.gradle.kts        # Root build configuration
├── settings.gradle.kts     # Gradle settings
├── gradle.properties       # Gradle properties (version, etc.)
├── mkdocs.yml              # Documentation configuration
├── pyproject.toml          # Python dependencies (uv)
├── README.md               # Project overview
├── CHANGELOG.md            # Repo-wide activity log
├── CONTRIBUTING.md         # Contribution guidelines
├── SECURITY.md             # Security policy
└── LICENSE                 # License
```

## Modules

- **terracotta-core**: Pure domain library with canonical models, provider interfaces, and semantic diff engine. Published to Maven Central as `io.github.beduality:terracotta-core`.
- **terracotta-state-filesystem**: File-backed state provider for persisting Terracotta state to disk.
- **terracotta-provider-modrinth**: Modrinth state and registry providers using Ktor Client and Kotlinx Serialization.
- **terracotta-provider-hangar**: Hangar state and registry providers using Ktor Client and Kotlinx Serialization.
- **terracotta-gradle-plugin**: Gradle plugin providing `terracottaPlan` and `terracottaApply` tasks with multi-provider support via ServiceLoader.
- **terracotta-github**: Pulumi infrastructure (Kotlin + Pulumi Java SDK) for managing GitHub repository settings and Action secrets.

## Project management

The `project/` directory is a git worktree checked out from the `project` branch.
It contains planning artifacts that evolve independently from `main`:

- **`TODO.md`**: Concrete, actionable tasks ready for implementation.
- **`BACKLOG.md`**: Ideas and tasks needing investigation or prioritization.
- **`plans/`**: Implementation plans created from `methodology/templates/plan.md`.
- **`designs/`**: Design proposals created from `methodology/templates/design.md`.
