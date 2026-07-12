---
description: Reusable workflow for generating an on-demand implementation plan from a TODO or backlog item.
---

# Module Plan Generation Workflow

A repeatable workflow for turning a `project/TODO.md` or `project/BACKLOG.md` item
into a focused, checkable plan. Plans are generated on demand, kept as small as
the change allows, and updated as work progresses.

## 1. Decide if a plan is needed

Create a plan file when the change is non-trivial or spans more than one module.
Skip for one-line fixes or documentation typos.

## 2. Choose the methodology path

Use the scope guidance in `module-development-workflow.md` to select the phase
sequence:

| Change type | Plan phases |
|---|---|
| New module / major feature / new public API | Brainstorm? → Design → Contract → Tests → Implementation → Review → Docs → Push and merge → Release report |
| Bug fix | Investigation → Tests → Implementation → Review (→ Docs → Push and merge → Release report if behavior changes) |
| Refactoring | Tests → Implementation → Review (→ Docs → Push and merge → Release report if shared) |
| Documentation-only | Docs → Review → Push and merge → Release report |

Insert **Contract** before Tests for bug fixes or refactorings that introduce a new public API surface or extension point.

## 3. Generate the plan file

Create `project/plans/YYYY-MM-<short-name>-plan.md` from `project/plans/TEMPLATE.md`.

Fill in:

- Source of truth (proposal, issue, TODO item).
- Scope and modules touched.
- Only the phases you actually need.
- Concrete checkboxes per phase.

Do not plan implementation details you cannot know yet. Stop at the next
unknown and add an investigation checkbox.

## 4. Work from the plan

Start with the first unchecked item. Do not check it off until the artifact is
produced and verified.

When a phase reveals new work:

- Add it as a checkbox in the right phase.
- If it invalidates an earlier phase, reopen its checkboxes and note why.

## 5. Update progress

Update the plan checkboxes while the session is in progress, as soon as an item
is completed or its status changes. Do not wait until the end of the session.

After every meaningful session, also review the plan as a whole:

- Check off completed items.
- Add new items discovered during work.
- Mark blocked items with `[~]` and a reason.

Keep the **Progress summary** table at the top accurate; it is the first thing
reviewers see.

## 6. Close the plan

When all checkboxes are done and the change is merged or released:

- Observe CD and confirm the deployment succeeds.
- Archive the plan, design proposal, and brainstorm note used for this work by moving them into `project/plans/archived/`, `project/designs/archived/`, and `project/brainstorm/archived/` respectively.
- Copy `project/reports/TEMPLATE.md` to `project/reports/release/<datetime>-<title>.md` and fill it out.
- Link the merged PR or release tag in the release report and any final notes.
- Move the source TODO item to a done section or remove it.

Do not keep finished plans open as living documents.
