# File Structure Reference

```
.
├── .agent/                 # Agent rules and guidelines
├── .devin/                 # Devin hooks and configuration
├── .github/                # GitHub Actions workflows and templates
├── .vscode/                # VSCode workspace settings and extensions
├── docs/                   # Documentation (MkDocs)
│   ├── content/            # Content organized by topic (sdk, gradle-plugin, repo)
│   └── hooks/              # Build hooks for copying generated docs
├── gradle/                 # Gradle wrapper and version catalogs
├── modules/                # Gradle modules
├── project/                # Project management
├── scripts/                # Utility scripts (release, deployment)
├── build.gradle.kts        # Root build configuration
├── settings.gradle.kts     # Gradle settings
├── gradle.properties       # Gradle properties (version, etc.)
├── mkdocs.yml              # Documentation configuration
├── pyproject.toml          # Python dependencies (uv)
├── README.md               # Project overview
├── CHANGELOG.md            # Release notes
├── CONTRIBUTING.md         # Contribution guidelines
├── SECURITY.md             # Security policy
└── LICENSE                 # License
```

## Modules

- **terracotta-core**: Pure domain library with canonical models, provider interfaces, and semantic diff engine. Published to Maven Central as `io.github.beduality:terracotta-core`.
- **terracotta-provider-modrinth**: Modrinth state and registry providers using Ktor Client and Kotlinx Serialization.
- **terracotta-gradle-plugin**: Gradle plugin providing `terracottaPlan` and `terracottaApply` tasks with multi-provider support via ServiceLoader.
- **terracotta-github**: Pulumi infrastructure (Kotlin + Java SDK) for managing GitHub repository settings and Action secrets.
