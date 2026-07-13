---
description: Simplify homepage, beginner tutorial, and getting-started docs to a single provider for clarity.
---

# Simplify Docs to a Single Provider

This plan follows `module-development-workflow.md` for a **documentation-only** change that
touches `docs`.

## Source of truth

- TODO item: None; this request is the source of truth.
- Design proposal: None.

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Documentation | Completed | Simplified to Modrinth as default single provider; tutorial renamed. |
| Review | Completed | `mkdocs build --strict` passes locally. |
| Push to remote | Completed | Pushed directly to `main`; Deploy docs workflow succeeded. |

## Phase 1: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Identify all docs that mention or demonstrate multiple providers:
  - `docs/index.md` (homepage)
  - `docs/content/integration/tutorials/publishing-to-multiple-providers.md` (beginner-facing tutorial)
  - `docs/content/modules/gradle-plugin/tutorials/getting-started.md` (getting-started guide)
- [x] Decide on the single provider to feature (default to Modrinth, the existing getting-started target).
- [x] Simplify `docs/index.md`:
  - Update hero copy and feature cards to describe one provider at a time.
  - Replace the minimal `terracotta.yml` example with a single-provider configuration.
  - Keep links to the Hangar how-to guide as a next step instead of front-and-center.
- [x] Refocus `docs/content/integration/tutorials/publishing-to-multiple-providers.md`:
  - Rewrite the tutorial as a single-provider walkthrough (default Modrinth).
  - Rename the file or add a redirect if the title changes.
  - Move multi-provider coverage to a follow-up how-to guide or a short "What's next" section.
- [x] Verify `docs/content/modules/gradle-plugin/tutorials/getting-started.md` already uses a single provider and only needs minor cross-link updates.
- [x] Update navigation links and cross-references that point to the renamed/rewritten tutorial.
- [x] Final verification: `mkdocs build --strict` passes locally.

## Phase 2: Review

- [x] Read `module-review-workflow.md`.
- [x] Run code review checklist.
- [x] By default, confirm auto-review (docs build) passes.
- [x] Update `CHANGELOG.md` if users need to know about the documentation restructure.

## Phase 3: Push to remote

- [x] Open a pull request to `main`.
- [x] Verify remote CI passes after the merge.

## Notes

- Renamed `publishing-to-multiple-providers.md` to `publishing-to-modrinth.md` and updated all internal references. No redirect plugin is configured, so this is a clean internal restructure.
