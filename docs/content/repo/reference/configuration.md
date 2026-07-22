# Configuration Reference

This reference covers all configuration files for the Terracotta project.

## Gradle Configuration

### Root Build Configuration (`build.gradle.kts`)

The root build file defines shared configuration for all subprojects.

**Key Configuration**:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.central.portal.publisher)
    jacoco
}
```

**Settings**:

- All projects share group `io.github.beduality`; each module has its own version in `modules/<module>/gradle.properties`
- Gradle plugins use version catalog from `gradle/libs.versions.toml`
- Spotless for code formatting
- Dokka for API documentation
- Central Portal Publisher for Maven Central publishing
- JaCoCo for test coverage

### Version Catalog (`gradle/libs.versions.toml`)

The version catalog defines all library versions in a single file.

**Available Versions**:

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 2.3.21 | JVM language |
| Ktor | 2.3.12 | HTTP client/server |
| kotlinx-serialization | 1.7.1 | JSON serialization |
| Spotless | 6.25.0 | Code formatting |
| Dokka | 1.9.20 | API documentation |
| JUnit | 5.10.2 | Testing framework |
| Pulumi | 1.30.0 | Infrastructure as code |
| JaCoCo | 0.8.12 | Test coverage |

**Available Plugins**:

| Plugin | Version | Purpose |
|--------|---------|---------|
| kotlin-jvm | 2.3.21 | Kotlin JVM target |
| kotlin-serialization | 2.3.21 | Kotlin serialization plugin |
| spotless | 6.25.0 | Code formatting |
| dokka | 1.9.20 | API documentation |
| central-portal-publisher | 0.2.0-alpha.1 | Maven Central publishing via Sonatype Central Portal |

## Documentation Configuration

### MkDocs (`mkdocs.yml`)

The MkDocs configuration defines site structure, theme, and navigation.

**Key Settings**:

```yaml
site_name: Terracotta
site_description: Declarative Minecraft project registry management tool
site_url: https://beduality.github.io/terracotta/
repo_url: https://github.com/beduality/terracotta
```

**Theme Configuration**:

- Material theme with custom directory
- Logo and favicon paths
- Dark/light palette with deep-purple primary color
- Features: instant navigation, search, tabs, code copying

**Markdown Extensions**:

- `pymdownx.highlight`: Syntax highlighting with line numbers
- `pymdownx.superfences`: Custom code fences (Mermaid diagrams)
- `pymdownx.tabbed`: Tabbed content
- `pymdownx.details`: Collapsible content blocks
- `admonition`: Alert boxes (note, warning, tip)
- `tables`: Markdown tables

**Plugins**:

- `search`: Full-text search
- `hooks`: Build-time hooks for copying generated docs

**Navigation Structure**:

- Quick Start
- Integration
- Modules
  - Core
  - Gradle Plugin
  - Modrinth Provider
  - Hangar Provider
- Repo
- Changes

## Module-Specific Configuration

### Gradle Plugin Module

The Gradle plugin module (`modules/terracotta-gradle-plugin`) has additional configuration:

**Plugin Metadata**:

- ID: `io.github.beduality.terracotta`
- Name: Terracotta
- Description: Declarative Minecraft project registry management
- Website: Project GitHub repository
- Version: From `gradle/libs.versions.toml`

### Provider Modules

Provider modules (`terracotta-provider-modrinth`) register their `ProviderFactory` via ServiceLoader:

```
META-INF/services/
└── io.github.beduality.terracotta.core.provider.ProviderFactory
```

The file contains the fully qualified class name of the factory implementation.

## Environment Variables

### Build Environment

| Variable | Purpose | Required |
|----------|---------|----------|
| `JAVA_HOME` | JDK 21 path | Yes (if system default differs) |

### Release Environment

| Variable | Purpose | Required |
|----------|---------|----------|
| `SONATYPE_USERNAME` | Sonatype Central Portal username | Yes (for releases) |
| `SONATYPE_PASSWORD` | Sonatype Central Portal password/token | Yes (for releases) |
| `SIGNING_KEY` | GPG private key (ASCII-armored) | Yes (for releases) |
| `SIGNING_PASSWORD` | GPG key passphrase | Yes (for releases) |

### API Tokens

| Variable | Purpose | Provider |
|----------|---------|----------|
| `MODRINTH_TOKEN` | Modrinth API token | Modrinth |
