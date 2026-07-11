---
description: Reusable workflow for adding user-facing documentation and KDoc cross-links for a Terracotta module.
---

# Module Documentation Workflow

A repeatable plan for documenting any `terracotta-*` module, keeping user docs,
reference docs, and API docs aligned without duplication.

This is Phase 4 of the module development workflow. Start with
`module-development-workflow.md` if you have not read it.

## 1. Create isolated module documentation

Add module-specific docs under `docs/content/<module>/`.

Structure them following the Diátaxis framework:

- `tutorials/` — learning-oriented, step-by-step lessons for newcomers.
- `how-to-guides/` — task-oriented recipes for common use cases.
- `reference/` — information-oriented descriptions of concepts, configuration, and public API surface.
- `explanation/` — understanding-oriented background and design rationale.

Keep prose focused on usage and intent. Do not copy full API signatures here.

## 2. Wire docs into site navigation

Open `mkdocs.yml` and add a dedicated navigation section for the module under `nav`.

Use human-readable titles and logical ordering (tutorials → how-to → reference → explanation).

## 3. Avoid duplicating API signatures

In reference pages, link to Dokka-generated KDoc instead of re-typing signatures.

Use the module's published KDoc URL (e.g. GitHub Pages Dokka output) for deep links.

## 4. Cross-link KDoc back to user docs

In the module source, add `@see` tags in the KDoc of every public class,
interface, object, enum, and top-level function, pointing to the relevant
GitHub Pages user docs.

This creates a bidirectional link between generated API docs and hand-written guides.

## 5. Fill member-level KDoc gaps

Ensure public API members have useful KDoc:

- Data-class properties: describe meaning, units, defaults, and constraints.
- Public functions: describe behavior, parameters, return values, and exceptions.

Goal: Dokka renders a useful page without requiring readers to open source files.

## 6. Deduplicate overlapping SDK / core / module docs

If the module's API is also covered elsewhere (for example in `docs/content/sdk/reference/api.md`),
trim that duplicated content and replace it with links to the new module docs.

Preserve the high-level SDK overview; move deep API details to the module-specific pages.

## 7. Verify changes

Run the module's tests and code-quality checks:

```bash
./gradlew :<module>:test :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply` to auto-fix formatting, then review the diff.

Also build the documentation site to confirm navigation and links are valid:

```bash
mkdocs build --strict
```

Fix any warnings or failures before proceeding.

## Module placeholders

For shared placeholders, see `README.md` in this directory.

Phase-specific placeholders:

| Placeholder | Example value |
|-------------|---------------|
| `docs/content/<module>/` | `docs/content/core/` |

---

## Related files

- `README.md` — shared placeholders and commit/release guidance.
- `mkdocs.yml` — site navigation and plugins.
- `docs/content/<module>/` — new user documentation.
- `modules/<module>/src/main/kotlin/` — source files receiving `@see` links and KDoc.
- `docs/content/sdk/reference/api.md` — example of content to deduplicate.
