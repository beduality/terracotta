## Changelog Guidelines (practical + strict)

Designed for projects where **consumers are developers, operators, and integrators (not just end-users)**.

---

# 1. Core Principle

A changelog records:

> **What changed that affects how the system is used, integrated, run, or depended on — and why it matters.**

It is **not**:

* commit history
* implementation diary
* internal refactoring log

---

# 2. Audience Model (important correction)

“End-user” is not a useful separator.

Use **impact-based audience instead**, organized under these standard subheadings in the changelog:

* **Players** (End-users) → players experiencing time formatting and dimension-aware messages
* **Server Administrators** (Operators) → deployers, server admins, CI/CD users configuring settings/releases
* **Developers** (API consumers) → library/plugin users, integrators depending on the code
* **Maintainers** → contributors and future engineers maintaining the codebase


If a change affects any of these → it belongs in the changelog.

---

# 3. Inclusion Rules

## MUST include if it affects:

### Behavior / runtime

* feature changes
* logic changes
* defaults
* compatibility shifts

### API / integration

* public API changes
* config schema changes
* serialization / data format changes

### Deployment / operations

* runtime environment changes
* build output changes
* installation / packaging changes
* required tooling changes

### Compatibility

* breaking changes
* deprecations
* version constraints

---

## MAY include (if externally relevant)

* CI/CD changes **only if they affect releases or artifacts**
* build system changes **only if output or compatibility changes**
* infrastructure changes **only if observable externally**

---

## MUST NOT include

* formatting/lint changes
* refactors without behavioral impact
* internal restructuring with no external effect
* dependency updates without impact (unless security/compatibility changes)

---

# 4. Structural Format

## Recommended: “What + Why”

Each entry should ideally contain:

### What (required)

* factual description of the change
* no implementation detail unless relevant

### Why (optional but strongly recommended)

* intent / motivation
* avoids ambiguity for future readers

---

## Optional: Technical Notes section

Use only when necessary:

* CI/CD
* build pipelines
* internal architecture changes

---

# 5. Section Model

Use structured sections:

## Added

New capabilities

## Changed

Behavior modifications (non-breaking or breaking depending on context)

## Fixed

Bug fixes

## Deprecated

Features scheduled for removal

## Removed

Deleted features

## Security

Vulnerabilities or security-related fixes

## Infrastructure (optional)

Only externally relevant internal system changes

---

# 6. CI/CD, Build, Chore Rules

## CI/CD

Include only if it affects:

* release process
* artifacts
* runtime validation
* production pipeline behavior

Exclude:

* YAML refactors
* job reordering without impact

---

## Build system

Include only if it affects:

* output format (jar, bundle, etc.)
* runtime compatibility
* performance characteristics
* supported environments

---

## Chore

Rule:

> Never include unless it has external behavior impact

Otherwise it is purely internal.

---

# 7. “What & Why” Style Rules

Preferred format:

```md
### Added

#### Players
- Locale-based time formatting  
  - Why: support heterogeneous client environments without manual configuration
```

Constraints:

* “What” must be short and concrete
* “Why” must explain user/operator benefit
* No implementation detail unless necessary for clarity

---

# 8. Writing Rules

* No commit messages
* No “refactored X to Y”
* No internal class names unless externally visible
* Avoid jargon unless it is part of public API

---

# 9. Breaking Changes

Must be explicit:

* mark clearly as breaking
* describe migration impact
* avoid burying in “Changed”

---

# 10. Mental Checklist (fast validation)

Before adding an entry:

* Does it affect usage, integration, runtime, or deployment?
* Can a consumer observe it?
* Does it change behavior or contracts?
* Does it require action from users/operators?

If all “no” → do not include.
