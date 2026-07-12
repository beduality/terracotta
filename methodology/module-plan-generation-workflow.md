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
| New module / major feature / new public API | Design → Tests → Implementation → Review → Docs |
| Bug fix | Investigation → Tests → Implementation → Review |
| Refactoring | Tests → Implementation → Review |
| Documentation-only | Docs → Review |

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

After every meaningful session, update the plan:

- Check off completed items.
- Add new items discovered during work.
- Mark blocked items with `[~]` and a reason.

Keep the **Progress summary** table at the top accurate; it is the first thing
reviewers see.

## 6. Close the plan

When all checkboxes are done and the change is merged or released:

- Add a final note with the outcome.
- Link the merged PR or release tag.
- Move the source TODO item to a done section or remove it.

Do not keep finished plans open as living documents.
