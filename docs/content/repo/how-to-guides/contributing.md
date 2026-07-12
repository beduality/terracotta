# Contributing

This guide shows how to take a task from the project board and land it in `main`.

## Before you start

You need a local clone of your fork with the upstream remote configured. If you have not done this yet, follow [Navigating the Codebase](../tutorials/navigating-the-codebase.md) first.

## 1. Pick a task

Open `project/TODO.md` and choose a task. If the task you want is not listed, propose it through the `project` branch first.

To claim a task, open a PR against `beduality/terracotta:project` that removes the task from `TODO.md`. Wait for that PR to merge before writing code.

## 2. Create a branch from `main`

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name
```

## 3. Make your changes

Implement the task. Follow the [Code Style Reference](../reference/code-style.md) and keep commits focused.

## 4. Verify locally

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew build
```

This compiles every module, runs all tests, and checks formatting.

## 5. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat(core): add new feature"
```

Common scopes: `core`, `modrinth`, `hangar`, `gradle-plugin`, `github`, `docs`, `repo`, `ci`.

## 6. Open a PR against `main`

Push your branch to your fork and open a pull request against `main`. Fill in the PR template and request review.

## PR checklist

Before requesting review, confirm:

- [ ] `./gradlew build` passes locally.
- [ ] `./gradlew test` passes.
- [ ] `./gradlew spotlessCheck` passes.
- [ ] Test coverage is maintained or improved.
- [ ] Documentation is updated if the change affects users or contributors.
- [ ] The PR description explains what changed and why.

## After merge

```bash
git checkout main
git pull upstream main
git branch -d feature/your-feature-name
```

## Where to get help

- [Code Style Reference](../reference/code-style.md)
- [Testing Guide](../how-to-guides/testing.md)
- [Architecture Overview](../../modules/core/explanation/architecture.md)
- [Discord](https://discord.gg/D5meCv2Wnd)