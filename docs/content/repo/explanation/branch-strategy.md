# Branch Strategy

Terracotta separates code history from planning history using two long-lived branches.

## Why two branches?

A single branch mixing code and planning creates noise. Release tags, CI runs, and documentation builds are interleaved with TODO edits, backlog grooming, and proposal iterations. By keeping planning on its own branch, `main` stays focused on shippable work while `project` retains a full audit trail of decisions.

## Why a git worktree?

Planning files would be invisible to contributors if they lived only on a remote branch. A worktree at `project/` makes them available locally without switching branches or polluting `main`.

- `main/` contains code and docs.
- `project/` contains planning artifacts.
- Both directories share the same git repository but track different branches.

This means you can edit a how-to guide on `main` and review a proposal on `project` without `git stash` or branch switches.

## How planning stays actionable

The `project` branch is not a free-form issue tracker. It has three strict buckets:

- **TODO.md**: Tasks ready to pick up.
- **BACKLOG.md**: Ideas not yet actionable.
- **designs/**: Larger changes needing design review.

This structure prevents the branch from becoming a dumping ground. If an item cannot be placed in one of these files with a clear next step, it is not ready for the `project` branch.

## Relationship to implementation

Planning PRs merge into `project`. Implementation PRs merge into `main`. A task should be removed from `TODO.md` on `project` before the corresponding code PR opens on `main`. This creates a clean handoff: the planning branch records the decision to do the work, and the main branch records the work itself.
