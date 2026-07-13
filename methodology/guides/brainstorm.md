---
description: Reusable workflow for a short, focused brainstorm before committing to a design direction.
---

# Module Brainstorm Workflow

A repeatable workflow for exploring alternatives, creative angles, and out-of-the-box ideas before committing to a design direction for a Terracotta module.

Typical path: open a brainstorm note → generate ideas and trade-offs → pick a direction or identify blockers → update the design proposal and plan.

## 1. Decide whether to brainstorm

Skip this phase when the change is small, well-understood, or has a clear precedent. Run it when:

- Multiple valid approaches exist.
- The change has broad consequences for the module or public API.
- The right direction is unclear from the TODO or issue alone.

## 2. Open a brainstorm note

Create `project/brainstorm/<datetime>-<title>.md` from `project/methodology/templates/brainstorm.md`.

Keep it succinct. The goal is sharper ideas, not a design document.

## 3. Generate ideas

Spend a short, time-boxed session:

- List at least two distinct approaches.
- Capture pros, cons, and open questions for each.
- Note trade-offs explicitly: what are you optimizing for and what are you giving up?
- Record risks and unknowns that could invalidate an approach.

## 4. Choose or block

End the session with one of:

- A recommended direction and the reasons it wins.
- A clear list of blockers or open questions that must be resolved before design can proceed.

## 5. Update downstream artifacts

If a direction was chosen:

- Update or create the design proposal in `project/designs/<module>-design.md`.
- Update `project/plans/<plan>.md` to reflect the chosen approach.

If the work is larger than expected, return to planning or split the work into smaller items.

## Output before proceeding

- A succinct brainstorm note, or a conscious decision to skip this phase.
- Updated design proposal and plan when a direction was selected.
