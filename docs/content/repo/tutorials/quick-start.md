# Quick Start

Get a local copy of Terracotta built and tested in under 10 minutes.

## Prerequisites

- Git 2.54+
- JDK 21
- (Optional) Python 3.13+ for documentation work

## 1. Clone and build

```bash
git clone https://github.com/beduality/terracotta.git
cd terracotta
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
```

`./gradlew build` compiles all modules, runs tests, checks formatting, and generates coverage reports.

## 2. Verify tests

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
```

## 3. Format your changes

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessApply
```

## What's next?

- [Navigating the Codebase](./navigating-the-codebase.md): Understand project structure and Git workflow.
- [Contributing](../how-to-guides/contributing.md): Learn the contribution process.
- [Building](../how-to-guides/building.md) and [Testing](../how-to-guides/testing.md): Detailed build and test options.
- [Documentation](../how-to-guides/documentation.md): Preview the docs site locally.
