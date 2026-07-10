# Building the Project

This guide covers building Terracotta for development and contribution.

## Prerequisites

- JDK 21 (the project targets JVM 17 but compiles under JDK 21)
- Gradle 8.0+

If your system default is a newer Java version, prefix commands with:
```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

## Build Commands

### Compile All Modules

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
```

This runs:

- Compilation of all modules
- All tests (unit, integration, and smoke tests)
- Code formatting verification (Spotless)
- Test coverage report generation

### Run Tests

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
```

### Generate API Documentation

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew dokkaHtml
```

Output is available at `build/dokka/html/index.html` in each module.

### Format Code

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessApply
```

### Check Code Formatting (CI requirement)

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessCheck
```

## Verifying Your Build

Run the complete verification pipeline:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew clean check
```

This ensures:

- All tests pass
- Code is properly formatted
- Test coverage meets thresholds
- Dokka documentation builds
