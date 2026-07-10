# Quick Start

This tutorial will get you up and running with Terracotta in under 10 minutes.

## What you'll learn

- How to clone and set up the repository locally
- How to build the project for the first time
- How to run tests to verify your setup
- How to start making changes

## Prerequisites

- Git 2.54.0 or later
- JDK 21
- Python 3.13+ (optional, for documentation)

## 1. Clone the Repository

Fork the repository on GitHub, then clone your fork locally:

```bash
git clone https://github.com/<your-username>/terracotta.git
cd terracotta
```

Add the upstream remote to pull in future changes:

```bash
git remote add upstream https://github.com/beduality/terracotta.git
```

## 2. Verify Your Java Setup

Terracotta requires JDK 21. Verify your setup:

```bash
java -version
# Should show openjdk version "21.x.x"
```

If your system default is a newer Java version, prefix commands with:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

## 3. Build the Project

Run the complete build pipeline to verify everything works:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew build
```

This command:

- Compiles all modules
- Runs all unit and integration tests
- Checks code formatting
- Generates coverage reports

If the build succeeds, you're ready to contribute!

## 4. Run Tests

Verify tests pass independently:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew test
```

## 5. Format Your Code

Before committing, ensure your code follows the project style:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew spotlessApply
```

## 6. Next Steps

- [Navigating the Codebase](./navigating-the-codebase.md): Understand the project structure in detail
- [Building and Testing](../how-to-guides/building.md): Learn about build options and verification
- [Testing Guide](../how-to-guides/testing.md): Understand test types and coverage expectations

## What's Next?

You now have a working local copy of Terracotta. Ready to make your first contribution?

1. Pick a task from `project/TODO.md`
2. Follow the [Contributing Guidelines](../reference/contributing-guidelines.md)
3. Create your PR against the `main` branch
