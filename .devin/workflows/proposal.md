---
description: Create a new project proposal from TEMPLATE.md and summarize it.
---

Triggered by `/proposal`.

1. Read `project/proposals/TEMPLATE.md`.
2. If the user did not provide a title, ask for a concise proposal title and a one-line description.
3. Ask the user for the proposal type (e.g., `add`, `change`, `design`, `narrow`, `refactor`) and category (e.g., `module`, `provider`, `config`, `cloud`, `state`, `workflow`, `project`, `task`, `platform`, `ide`).
4. Determine the filename as `YY-MM-DD-type-category-name.md`, using the current date and the chosen type and category.
5. If a file at `project/proposals/<filename>` already exists, warn the user and ask for confirmation before overwriting.
6. Create the proposal file following `TEMPLATE.md` exactly:
   - Fill the YAML `description` frontmatter with the one-line summary.
   - Replace the `# TITLE` heading with a descriptive title.
   - Keep all section headings from the template.
   - Fill each section with relevant, project-specific content.
   - Use code snippets or tables when they clarify the design.
7. If the proposal was generated from a `project/TODO.md` item, update that item in `project/TODO.md` to follow the existing link pattern:
   - Preserve any leading tag (e.g., `[gha]`) and action verb.
   - Replace the descriptive text with a Markdown link to the new proposal using the proposal title: `[Title](./proposals/YY-MM-DD-type-category-name.md)`.
   - Example: `- [gha] Add terracotta-gha module for GHA integration` becomes `- [gha] Add [Terracotta GHA Module](./proposals/26-07-12-add-module-gha.md)`.
8. After writing the file, produce a concise summary (TL;DR) of the proposal at the end of your response, including the file path.
