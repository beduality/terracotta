# Project Files Reference

Planning files live in the `project/` git worktree, which tracks the `project` branch.

## `TODO.md`

Concrete, actionable tasks that are ready for implementation.

- Items are removed in the planning PR before implementation starts.
- Each item should be small enough to finish in one or a few commits.
- Format is free-text Markdown; keep it scannable.

## `BACKLOG.md`

Ideas and tasks that are not yet ready for implementation.

- Use this for raw ideas, future investigations, and items waiting on other work.
- Move items to `TODO.md` once they are well-defined and actionable.

## `project/designs/`

Larger changes that need discussion before implementation.

A proposal should include:

- **Problem**: What are you solving?
- **Solution**: What do you propose?
- **Alternatives**: What else did you consider?
- **Open questions**: What still needs decision?

Proposals are accepted or rejected through PR review against the `project` branch.

## Planning workflow

1. Open a PR against `project` to add, move, or remove items.
2. After the planning PR merges, create a code branch from `main` for implementation.
3. Remove the task from `TODO.md` in a planning PR before starting the implementation PR.

See [Project Management](../explanation/project-management.md) for why planning happens through PRs.
