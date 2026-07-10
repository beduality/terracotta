# Navigating the Codebase

This tutorial will help you understand the Terracotta project structure and get started contributing.

## What you'll learn

- How to clone and set up the repository locally
- The project's Git workflow and branch strategy
- How project management works with TODO, BACKLOG, and proposals
- How to navigate the multi-module Gradle structure

## Prerequisites

- Git 2.54.0 or later
- JDK 21 for building
- Python 3.13+ for documentation (optional)

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

## 2. Understand the Branch Strategy

The project uses two main branches:

- **`main`**: The development branch with the latest code and documentation
- **`project`**: A separate branch used for project management (TODO, BACKLOG, proposals)

The `project` branch is checked out as a git worktree at `project/` from the main branch. This keeps planning separate from code while maintaining version history.

## 3. Project Management Workflow

All project planning happens through Pull Requests against the `project` branch. There are no external issue trackers.

### TODO

`TODO.md` contains concrete, actionable tasks ready to be worked on. To pick up a task:

1. Open a PR against the `project` branch
2. Remove the task from `TODO.md` in that PR
3. Wait for merge before starting implementation

### Backlog

`BACKLOG.md` contains ideas and tasks not yet ready for implementation. Move items to TODO when they become well-defined and actionable.

### Proposals

For larger changes, create a proposal in `project/proposals/`:

1. Create a Markdown file with a descriptive name
2. Include: problem, solution, alternatives, open questions
3. Open a PR against `project` containing only the proposal
4. Iterate through review until accepted or rejected

## 4. Module Structure

The project is organized as a multi-module Gradle build under `modules/`:

```
modules/
├── terracotta-core/              # Pure domain library
├── terracotta-provider-modrinth/ # Modrinth registry integration
├── terracotta-gradle-plugin/     # Gradle plugin frontend
└── terracotta-github/            # Pulumi infrastructure
```

### terracotta-core

Contains canonical models, provider interfaces, and the semantic diff engine. Depends only on standard Kotlin/Java APIs - no network or framework dependencies.

### terracotta-provider-modrinth

Implements registry-specific behaviors for Modrinth. Depends on Ktor Client and Kotlinx Serialization.

### terracotta-gradle-plugin

Build-tool integration providing `terracottaPlan` and `terracottaApply` tasks. Uses ServiceLoader for provider discovery.

### terracotta-github

Manages GitHub repository configuration and secrets using Pulumi Java/Kotlin.

## 5. Next Steps

- [Building and Testing](../how-to-guides/building.md): Learn how to compile and test
- [Testing Guide](../how-to-guides/testing.md): Understand test types and coverage
- [Contributing](../how-to-guides/contributing.md): Review contribution guidelines
