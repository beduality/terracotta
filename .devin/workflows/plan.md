---
description: Reusable workflow for generating an on-demand implementation plan from a TODO or backlog item.
---

Triggered by `/plan`.

This workflow follows `project/methodology/module-plan-generation-workflow.md`.

1. **Decide if a plan is needed.**
   - If the change is trivial (one-line fix, typo, or single-file refactor), suggest skipping the plan and doing the work directly.
   - Otherwise proceed.

2. **Collect inputs.**
   - Ask for a concise plan title and a one-line description, unless already provided.
   - Ask for the change type unless already provided:
     - New module / major feature / new public API
     - Bug fix
     - Refactoring
     - Documentation-only
   - Ask for the modules this plan touches, unless already provided.
   - Ask for the source of truth (TODO item, issue, or proposal) if known.

3. **Choose the phase sequence.**
   - Use the scope guidance in `project/methodology/module-development-workflow.md`:

     | Change type | Plan phases |
     |-------------|-------------|
     | New module / major feature / new public API | Design → Tests → Implementation → Review → Docs |
     | Bug fix | Investigation → Tests → Implementation → Review |
     | Refactoring | Tests → Implementation → Review |
     | Documentation-only | Docs → Review |

4. **Read `project/plans/TEMPLATE.md`.**

5. **Determine the filename.**
   - Use `YYYY-MM-<kebab-case-title>-plan.md` based on current year and month.
   - If `project/plans/<filename>` already exists, warn the user and ask for confirmation before overwriting.

6. **Create the plan file from `TEMPLATE.md`.**
   - Fill the YAML `description` frontmatter with the one-line summary.
   - Replace the `# <Title>` heading with the descriptive title.
   - Fill the intro sentence with the change type and affected modules.
   - Fill the source-of-truth references (TODO item or proposal) when provided; otherwise leave placeholders.
   - Include **only the phases needed** for the selected change type:
     - Keep matching template phases as-is, mapped to the chosen sequence.
     - For **Bug fix**, prepend a `Phase 0: Investigation` checklist before the TDD phase.
     - Omit phases the change type does not need (e.g., remove System design for refactoring and bug fixes, remove Documentation for bug fixes unless behavior changes).
   - Set the progress table to include only the selected phases, all `Not started`.
   - Leave the `## Notes` section ready for discoveries and blockers.

7. **Summarize the created plan.**
   - Report the file path, change type, affected modules, and selected phases.
   - Remind the user to update checkboxes and the progress summary while work progresses, and to close the plan when the change is merged or released.
