---
description: Reusable workflow for adding user-facing documentation and KDoc cross-links for a Terracotta module.
---

# Module Documentation Workflow

A repeatable plan for documenting any `terracotta-*` module, keeping user docs,
reference docs, and API docs aligned.

This is Phase 7 of the module development workflow. Start with
`development.md` if you have not read it.

## 1. Create isolated module documentation

Add module docs under `docs/content/<module>/` using the Diátaxis framework:

- `tutorials/` — step-by-step lessons.
- `how-to-guides/` — task-oriented recipes.
- `reference/` — concepts, configuration, and public API surface.
- `explanation/` — background and design rationale.

Keep prose focused on usage and intent; do not copy full API signatures.

## 2. Wire docs into site navigation

Add a dedicated navigation section for the module in `mkdocs.yml` under `nav`,
using readable titles and logical ordering.

## 3. Avoid duplicating API signatures

Link to Dokka-generated KDoc instead of re-typing signatures. Use the module's
published KDoc URL for deep links.

## 4. Cross-link KDoc back to user docs

Add `@see` tags in KDoc for every public class, interface, object, enum, and
top-level function, pointing to the relevant user docs. This creates a
bidirectional link between generated API docs and hand-written guides.

## 5. Fill member-level KDoc gaps

Ensure public API members have useful KDoc:

- Data-class properties: meaning, units, defaults, constraints.
- Public functions: behavior, parameters, return values, exceptions.

Goal: Dokka renders a useful page without requiring readers to open source files.

## 6. Deduplicate overlapping SDK / core / module docs

If the API is also covered elsewhere (for example in `docs/content/sdk/reference/api.md`),
trim duplication and replace it with links to the module docs. Preserve the
high-level SDK overview; move deep API details to the module-specific pages.

## 7. Verify changes

Run tests and quality checks:

```bash
./gradlew :<module>:test :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply`, then review the diff.

Build the docs site to confirm navigation and links:

```bash
mkdocs build --strict
```

Fix warnings or failures before proceeding.

