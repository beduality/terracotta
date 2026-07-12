# First Contribution

This tutorial walks you through a complete first contribution, from reading `TODO.md` to merging your change into `main`.

## What you'll learn

- How to claim a task from the `project` branch.
- How to create a feature branch, make a change, and verify it.
- How to open and merge a pull request.

## Prerequisites

- A local clone with the `project` worktree set up. See [Navigating the Codebase](./navigating-the-codebase.md).
- A successful `./gradlew build`. See [Quick Start](./quick-start.md).

## 1. Pick a task

Open `project/TODO.md` and choose a small, well-defined task. For your first contribution, look for something labeled as good for newcomers, such as a docs improvement or a small refactor.

To claim the task, open a PR against `beduality/terracotta:project` that removes the task from `TODO.md`. Wait for that PR to merge before you write code.

## 2. Create a branch

Update your local `main` and create a branch:

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-change-name
```

## 3. Make the change

Edit the relevant files. If you are changing code, follow the [Code Style Reference](../reference/code-style.md). If you are changing docs, follow the [Documentation Style Reference](../reference/documentation-style.md).

Keep the change focused. A good first PR changes one thing.

## 4. Add a changelog entry

If your change affects users, operators, or contributors, add an entry under `## [Unreleased]` in `CHANGELOG.md`:

```md
### Fixed

**Docs**

- Fixed unclear navigation in the repo tutorials so new contributors can find the first contribution guide.
```

See [Writing Changelog Entries](../how-to-guides/writing-changelog.md) for the format.

## 5. Verify locally

Run the verification pipeline:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew build
```

For docs-only changes, you can also preview the site:

```bash
uv run mkdocs serve
```

## 6. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git add .
git commit -m "docs(repo): clarify first contribution path"
```

## 7. Push and open a PR

Push your branch to your fork and open a PR against `beduality/terracotta:main`.

Fill in the PR template and link to the planning PR that removed the task from `TODO.md`.

## 8. Merge and clean up

After review, a maintainer will merge your PR. Update your local `main` and delete the branch:

```bash
git checkout main
git pull upstream main
git branch -d feature/your-change-name
```

## What's next?

- [Contributing](../how-to-guides/contributing.md): The full contributor workflow.
- [Testing](../how-to-guides/testing.md): How to run specific test suites.
- [Repo Architecture](../explanation/architecture.md): How the modules fit together.
