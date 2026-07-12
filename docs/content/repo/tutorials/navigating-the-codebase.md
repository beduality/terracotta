# Navigating the Codebase

This tutorial walks you through cloning the repository, setting up the project worktree, and finding your way around the codebase.

## What you'll learn

- How to clone the repository and add the upstream remote
- How to check out the `project` worktree for planning files
- Where the Gradle modules, tests, and documentation live
- How to run a full local build

## Prerequisites

- Git 2.54.0 or later
- JDK 21 for building

## 1. Clone and add upstream

Fork the repository on GitHub, then clone your fork locally:

```bash
git clone https://github.com/<your-username>/terracotta.git
cd terracotta
```

Add the upstream remote:

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
- `terracotta/project/` tracks the `project` branch and contains `TODO.md`, `BACKLOG.md`, and `proposals/`.

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

## 4. Build the project

Run a full build to verify your environment:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
```

This compiles every module, runs tests, checks formatting, and generates coverage reports. When it finishes successfully, you are ready to make changes.

## What's next?

- [Contributing](../how-to-guides/contributing.md): Pick up a task and open a PR.
- [Building](../how-to-guides/building.md): Detailed build commands.
- [Testing](../how-to-guides/testing.md): Run specific test suites.
- [Project Files](../reference/project-files.md): How TODO, BACKLOG, and proposals work.
- [Branch Strategy](../explanation/branch-strategy.md): Why planning lives on its own branch.
