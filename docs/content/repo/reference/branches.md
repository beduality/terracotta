# Branches Reference

Terracotta uses two long-lived branches with different responsibilities.

## `main`

The default development branch. Contains the latest code, documentation, and release metadata.

| Aspect | Value |
|---|---|
| Default branch | Yes |
| Purpose | Active development and releases |
| Protection | Pull requests required for changes |
| Where it deploys | Documentation `unreleased` alias on every push |

## `project`

A dedicated branch for project management. It is checked out as a git worktree at `project/` from the `main` branch.

| Aspect | Value |
|---|---|
| Default branch | No |
| Purpose | TODO, BACKLOG, and proposals |
| Where it lives | `project/` worktree on a local clone |
| How to update | Open PRs against `beduality/terracotta:project` |

## Worktree setup

The `project/` directory is a git worktree of the `project` branch. This keeps planning files out of the `main` branch history while keeping them versioned.

```bash
# From a fresh clone on main
git fetch origin project
git worktree add project origin/project
```

## Short-lived branches

Contributor branches should be created from `main` for code and from `project` for planning changes.

| Branch type | Base branch | Naming example |
|---|---|---|
| Feature | `main` | `feature/your-feature` |
| Fix | `main` | `fix/short-description` |
| Planning | `project` | `plan/task-description` |

For the reasoning behind this model, see [Project Management](../explanation/project-management.md) and [Branch Strategy](../explanation/branch-strategy.md).
