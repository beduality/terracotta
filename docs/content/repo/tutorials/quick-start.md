# Quick Start

Get a local copy of Terracotta built and tested in under 10 minutes.

## Prerequisites

- Git 2.54+
- JDK 21

## 1. Clone the repository

```bash
git clone https://github.com/beduality/terracotta.git
cd terracotta
```

## 2. Build and verify

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
```

`./gradlew build` compiles all modules, runs tests, checks formatting, and generates coverage reports. When it finishes, your local environment is ready.

## What's next?

- [Navigating the Codebase](./navigating-the-codebase.md): Set up the project worktree and learn where everything lives.
- [Contributing](../how-to-guides/contributing.md): Pick up a task and open a PR.
- [Building](../how-to-guides/building.md) and [Testing](../how-to-guides/testing.md): Detailed build and test options.
- [Documentation](../how-to-guides/documentation.md): Preview the docs site locally.
