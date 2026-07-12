---
description: Create a new project proposal from TEMPLATE.md and summarize it.
---

Triggered by `/proposal`.

1. Read `project/proposals/TEMPLATE.md`.
2. If the user did not provide a title, ask for a concise proposal title and a one-line description.
3. Determine the filename as `YYYY-MM-<kebab-case-title>.md`, using the current year and month.
4. If a file at `project/proposals/<filename>` already exists, warn the user and ask for confirmation before overwriting.
5. Create the proposal file following `TEMPLATE.md` exactly:
   - Fill the YAML `description` frontmatter with the one-line summary.
   - Replace the `# TITLE` heading with a descriptive title.
   - Keep all section headings from the template.
   - Fill each section with relevant, project-specific content.
   - Use code snippets or tables when they clarify the design.
6. After writing the file, produce a concise summary (TL;DR) of the proposal at the end of your response, including the file path.
