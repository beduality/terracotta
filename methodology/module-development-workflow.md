---
description: Parent workflow that coordinates designing, testing, implementing, reviewing, documenting, and bug-fixing a Terracotta module.
---

# Module Development Workflow

A high-level workflow for building or significantly changing any `terracotta-*`
module. It coordinates focused child workflows and defines the order in which they
run.

For shared conventions, see `README.md` in this directory.

Main sequence: `System design → Tests → Implementation → Review → Documentation`

Bug-fix sequence: `Investigation → Tests → Implementation → Review`

Every change passes through **Review** before release.

---

## Phase 1: System design

**Goal**: define responsibilities, public API, and system fit.

**Read**: `project/methodology/module-system-design-workflow.md`

**Output before proceeding**:

- Design proposal in `project/proposals/<module>-design.md`.
- Scope: responsibilities, inputs, outputs, and side effects.
- Public API contract with Kotlin signatures or pseudo-code.
- Internal component boundaries and dependency rules.
- Error-handling and configuration strategy.

**Stop if**: the proposal has not been reviewed, or the module's responsibilities
overlap with an existing module.

---

## Phase 2: Test-driven development

**Goal**: write executable specifications before production code exists.

**Read**: `project/methodology/module-testing-workflow.md`

**Input from design phase**:

- Public API contract.
- Edge cases and error conditions.
- Module boundaries and seams for isolated testing.

**Output before proceeding**:

- Failing tests in `modules/<module>/src/test/kotlin/`.
- Confirmation that each test fails for the expected reason.

**Stop if**: tests are written against an unstable API, or if the tests reveal
that the design needs to change. In that case, return to Phase 1.

---

## Phase 3: Implementation

**Goal**: make failing tests pass with a production-ready implementation.

**Read**: `project/methodology/module-implementation-workflow.md`

**Input from TDD phase**:

- Failing tests that describe behavior.
- A stable public API contract.

**Work**:

- Write the smallest implementation that turns tests green.
- Refactor for composability, modularity, extensibility, abstraction, and type safety.
- Document public API with KDoc for Dokka.
- Verify the full module build and quality checks.

**Output before proceeding**:

- Passing tests and a green `:build`.
- A clean public API with KDoc.
- Updated `build.gradle.kts` if new dependencies or tasks are needed.

**Stop if**: the implementation repeatedly forces API changes that invalidate
the design. Return to Phase 1, then Phase 2.

---

## Phase 4: Documentation

**Goal**: publish user-facing guides and cross-linked API reference.

**Read**: `project/methodology/module-documentation-workflow.md`

**Input from implementation phase**:

- Stable public API with KDoc.
- Behavior demonstrated by passing tests.

**Work**:

- Write Diátaxis docs under `docs/content/<module>/`.
- Wire docs into `mkdocs.yml`.
- Cross-link KDoc to user docs with `@see` tags.
- Deduplicate content from SDK or shared reference pages.
- Verify docs build and links work.

**Output before considering the module complete**:

- Merged user docs in `docs/content/<module>/`.
- Cross-linked KDoc.
- Passing `./gradlew :<module>:test :<module>:spotlessCheck` and `mkdocs build --strict`.

---

## Hand-off checklist

| From | To | Required artifact |
|------|----|-------------------|
| Design | Tests | Approved design proposal with public API sketch |
| Tests | Implementation | Failing tests exercising the public API contract |
| Implementation | Review | Passing tests and KDoc-covered public API |
| Investigation | Tests | Reproduced bug and regression test |
| Review | Docs | Approved change |
| Docs | Release | Built docs and green module checks |

---

## Scope guidance

Not every module change needs every phase.

| Change type | Phases needed |
|-------------|---------------|
| New module | Design → Tests → Implementation → Review → Docs |
| Major feature | Design → Tests → Implementation → Review → Docs |
| New public API or extension point | Design → Tests → Implementation → Review → Docs |
| Bug fix | Investigation → Tests → Implementation → Review (→ Docs if behavior changes) |
| Refactoring | Tests → Implementation → Review |
| Documentation-only | Docs → Review |

---

## Review gates

Run `module-review-workflow.md` before moving forward:

- **Design review** before implementation.
- **Code review** before docs or merge.
- **Documentation review** before release.
- **Merge / release review** before tagging or deploying.

Review feedback can loop a change back to an earlier phase.
