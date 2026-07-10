# Project Management

This page explains how Terracotta handles project planning and management.

## Philosophy

This project uses a **PR-driven planning model** instead of external issue trackers or project boards. All planning happens through Pull Requests against the `project` branch.

### Why PR-Driven Planning?

- **Transparency**: Every planning decision is visible in git history
- **Context**: PR discussions provide rich context for future maintainers
- **Alignment**: Planning and implementation are the same workflow
- **Version Control**: Planning changes are tracked like any other change

## The Project Branch

The `project` branch is a separate branch used exclusively for project management. It is checked out as a git worktree at `project/` from the main branch.

This separation keeps:

- **Main branch**: Focused on code and documentation
- **Project branch**: Focused on planning and tasks

## Files and Folders

### TODO.md

Contains concrete, actionable tasks ready to be worked on.

**Content**:

- Well-defined tasks with clear acceptance criteria
- Tasks that have been groomed and prioritized
- Tasks ready for implementation

**Workflow**:

1. Open a PR against `project` removing the task
2. Wait for merge (signals others the task is being worked on)
3. Then implement and submit the PR

**Rules**:

- Remove from TODO when a PR targeting `project` removes it
- Do not implement before the removal PR merges (prevents duplicate work)

### BACKLOG.md

Contains ideas and tasks not yet ready for implementation.

**Content**:

- Ideas that need more investigation
- Tasks that require discussion or prioritization
- Dependent work that isn't ready
- Large features split into smaller tasks

**Workflow**:

- Items stay in BACKLOG until well-defined and actionable
- Move to TODO when ready for implementation
- No PR is needed just to add items to BACKLOG

### project/proposals/

Contains proposals for larger changes that should be discussed before implementation.

**Structure**:

```
project/proposals/
├── yaml-schema-support.md
├── cli-command-rename.md
└── new-provider-interface.md
```

**Each proposal should include**:

- The problem being solved
- The proposed solution
- Alternatives considered
- Open questions or unresolved decisions

**Workflow**:

1. Create a proposal file with a descriptive name
2. Open a PR against `project` containing only the proposal
3. Iterate through review until accepted or rejected
4. Begin implementation only after acceptance

## Planning Workflow

### Starting Work on a Task

1. Check `TODO.md` for an existing task
2. If not found, add it to `TODO.md` (with PR against `project`)
3. Open a PR against `project` removing the task from TODO
4. Wait for merge (signals the task is being worked on)
5. Implement and submit the PR

### Proposing Large Changes

1. Create a proposal in `project/proposals/`
2. Open a PR against `project` containing only the proposal
3. Iterate through review
4. Accept/reject the proposal
5. If accepted, create tasks in TODO.md

### Grooming the Backlog

1. Review `BACKLOG.md` periodically
2. Move items to TODO when they become actionable
3. Refine or split large items
4. Remove items that are no longer relevant

## Relationship to GitHub Issues

This project does **not** use GitHub Issues. All planning and tracking happens in the `project` branch:

- Bugs → Add to TODO or BACKLOG
- Feature requests → Add to TODO or BACKLOG
- Enhancements → Add to TODO or BACKLOG
- Proposals → Add to `project/proposals/`

If you find a bug or have a feature request, open an issue on GitHub. We'll convert it to a planning item in the `project` branch.
