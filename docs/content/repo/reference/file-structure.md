# File Structure Reference

```
.
├── docs/                   # Documentation (MkDocs)
│   ├── content/            # Content organized by topic (cli, sdk, config, repo)
│   ├── hooks/              # Build hooks for copying generated docs
│   └── overrides/          # Theme customizations
├── guidelines/             # Contribution guidelines
├── modules/                # Gradle modules
├── project/                # Project management (TODO, BACKLOG)
├── scripts/                # Utility scripts (release, deployment)
├── build.gradle.kts        # Root build configuration
├── settings.gradle.kts     # Gradle settings
└── mkdocs.yml              # Documentation configuration
```

## Modules

- **terracotta-core**: Pure domain library with canonical models, provider interfaces, and semantic diff engine. Published to Maven Central as `io.github.beduality:terracotta-core`.
- **terracotta-provider-modrinth**: Modrinth state and registry providers using OkHttp and Jackson.
- **terracotta-cli**: Command-line interface using Picocli, compiled to native binaries via GraalVM.
- **terracotta-github**: Pulumi infrastructure (Kotlin + Java SDK) for managing GitHub repository settings and Action secrets.
