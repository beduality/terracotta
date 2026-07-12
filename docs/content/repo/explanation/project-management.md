# Project Management

Terracotta uses a **PR-driven planning model** instead of external issue trackers or project boards. Planning happens through Pull Requests against the `project` branch, which is checked out as a git worktree at `project/` from `main`.

## Why PR-driven planning?

- **Transparency**: Every planning decision is visible in git history and reviewable like code.
- **Context**: PR discussions capture the reasoning behind tasks and proposals for future maintainers.
- **Alignment**: Planning and implementation use the same workflow, so decisions are made by the same people who implement them.
- **Version control**: Planning changes are tracked, reverted, and audited the same way as code changes.

## Why a separate `project` branch?

Keeping planning on a dedicated branch means:

- `main` stays focused on released code and documentation.
- `project/` can evolve independently without polluting the main line.
- Task ownership, backlog grooming, and proposals can be merged quickly without affecting builds.

## Planning artifacts

- **`TODO.md`**: Concrete, actionable tasks that are ready for implementation.
- **`BACKLOG.md`**: Ideas and tasks that need more investigation or prioritization.
- **`project/proposals/`**: Larger changes that need discussion before implementation.

For the mechanics of moving items between these files and opening planning PRs, see the [Contributing how-to guide](../how-to-guides/contributing.md).
