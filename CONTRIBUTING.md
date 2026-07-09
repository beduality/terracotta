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

---

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
