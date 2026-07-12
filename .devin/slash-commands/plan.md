---
description: Create a new implementation plan from project/plans/TEMPLATE.md and summarize it.
---

Triggered by `/plan`.

1. Read `project/plans/TEMPLATE.md`.
2. If the user did not provide a title, ask for a concise plan title and a one-line description.
3. Ask for the change type and the modules this plan touches, unless the user already provided them.
4. Determine the filename as `YYYY-MM-<kebab-case-title>-plan.md`, using the current year and month.
5. If a file at `project/plans/<filename>` already exists, warn the user and ask for confirmation before overwriting.
6. Create the plan file from `TEMPLATE.md`:
   - Fill the YAML `description` frontmatter with the one-line summary.
   - Replace the `# <Title>` heading with a descriptive title.
   - Fill the intro sentence with the change type and affected modules.
   - Fill the TODO item and design proposal references if provided by the user; otherwise leave them as placeholders.
   - Keep the progress table with all phases set to `Not started`.
   - Keep the phase checklists as they appear in the template.
   - Leave the `## Notes` section ready for discoveries and blockers.
7. After writing the file, produce a concise summary of the plan at the end of your response, including the file path and the modules it covers.
