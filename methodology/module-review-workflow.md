---
description: Reusable review checkpoints for design, code, and documentation changes.
---

# Module Review Workflow

A set of review checkpoints for any change that moves between phases of the
module development workflow or toward release.

Run the relevant checklist before merging or releasing.

## 1. Design review

Before approving a design proposal:

- The problem and scope are clearly stated.
- The public API is small and stable.
- Responsibilities do not duplicate another module.
- Dependency directions follow the project rules (no cycles; internal packages and symbols marked).
- Error handling and configuration strategies are explicit.
- Open questions and risks are listed.

## 2. Code review

Before approving an implementation:

- All new behavior is covered by tests.
- Tests are readable, isolated, focused, and maintainable.
- The fix or feature is minimal and does not include unrelated changes.
- Public API has KDoc; internal code is self-explanatory.
- No secrets, tokens, or sensitive data are logged or exposed.
- `spotlessCheck` passes.
- The full module build passes (`:<module>:build`).

## 3. Documentation review

Before approving documentation:

- New docs follow the Diátaxis framework.
- API signatures are not duplicated; KDoc is linked instead.
- `@see` tags in KDoc point to the correct user docs.
- `mkdocs build --strict` passes.
- Spelling and grammar are correct.

## 4. Merge / release review

Before merging or releasing:

- The change matches the scope guidance in `module-development-workflow.md`.
- `CHANGELOG.md` is updated if users need to know about the change.
- CI is green.

---

## Related files

- `module-development-workflow.md` — scope guidance and phase ordering.
- `README.md` — shared placeholders and commit/release guidance.
