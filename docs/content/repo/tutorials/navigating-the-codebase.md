# Navigating the Codebase

This tutorial walks you through orienting yourself in the repository and setting up the planning worktree. It assumes you already have a local clone and a successful build from [Quick Start](./quick-start.md).

## What you'll learn

- How to add the upstream remote
- How to check out the `project` worktree for planning files
- Where the Gradle modules, tests, and documentation live
- How to verify your local environment with a targeted build

## Prerequisites

- A local clone of your fork
- JDK 21
- A successful run of `./gradlew build` from Quick Start

## 1. Add the upstream remote

If you cloned your own fork, add the upstream remote so you can pull in future changes:

```bash
git remote add upstream https://github.com/beduality/terracotta.git
```

## 2. Check out the project worktree

Terracotta keeps planning files on a separate `project` branch. Check it out as a worktree at `project/`:

```bash
git fetch origin project
git worktree add project origin/project
```

You now have two views of the same repository:

- `terracotta/` tracks `main` and contains code and docs.
- `terracotta/project/` tracks the `project` branch and contains `TODO.md`, `BACKLOG.md`, and `designs/`.

## 3. Find your way around

Open the repository in your editor. The important top-level directories are:

```
terracotta/
├── docs/                   # MkDocs site
├── modules/                # Gradle modules
│   ├── terracotta-core/
│   ├── terracotta-gradle-plugin/
│   ├── terracotta-github/
│   ├── terracotta-provider-modrinth/
│   └── terracotta-provider-hangar/
├── scripts/                # Release and infrastructure scripts
├── project/                # Planning worktree
└── .github/workflows/      # CI/CD workflows
```

For a detailed file map, see [File Structure](../reference/file-structure.md).

## 4. Verify a module builds in isolation

Run a targeted build on `terracotta-core` to confirm you can build a single module:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :terracotta-core:build
```

This is faster than a full build and confirms your local environment is ready to work on a specific module.

## What's next?

- [Contributing](../how-to-guides/contributing.md): Pick up a task and open a PR.
- [Building](../how-to-guides/building.md): Detailed build commands.
- [Testing](../how-to-guides/testing.md): Run specific test suites.
- [Project Files](../reference/project-files.md): How TODO, BACKLOG, and proposals work.
- [Branch Strategy](../explanation/branch-strategy.md): Why planning lives on its own branch.
