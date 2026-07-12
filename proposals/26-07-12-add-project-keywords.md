---
description: Add a free-form keywords field to Terracotta projects and map it to Hangar settings.keywords and GitHub repository topics.
---

# Add Project Keywords

## TL;DR

Add a `keywords: List<String>` field to the canonical `TerracottaProject` model, emit `UpdateKeywords` operations when it changes, and map those keywords to Hangar's free-form search keywords and GitHub repository topics. Modrinth and CurseForge do not support free-text keywords, so those providers skip the operation with a warning.

## Problem Statement

Hangar exposes a **Keywords** field in project settings that improves search discoverability. It is separate from **Tags** (which are boolean flags like `SUPPORTS_FOLIA`) and from **Category** (a single controlled value). The current Terracotta model has no place for these free-form search terms, so users must add them manually after a release.

GitHub has a similar concept called **repository topics**. The upcoming GitHub provider proposal already plans to map `project.tags` to topics, but once the [Narrow Tags](./2025-07-narrow-tags.md) proposal replaces free-form tags with structured categories, topics will need a different canonical source. Keywords are the natural source.

## Goals

1. Add `keywords: List<String>` to `TerracottaProject` and the Gradle/YAML configuration.
2. Emit `Operation.UpdateKeywords` when the keyword list changes.
3. Map keywords to Hangar `settings.keywords` (max 5).
4. Map keywords to GitHub repository topics via `PUT /repos/{owner}/{repo}/topics`.
5. Keep the implementation separate from the structured `tags`/`categories` work in [Narrow Tags](./2025-07-narrow-tags.md).

## Non-Goals

- Replacing or renaming the existing `tags` field. That is covered by [Narrow Tags](./2025-07-narrow-tags.md).
- Supporting keywords on Modrinth or CurseForge. Neither platform exposes a free-text keyword field.
- Validating keyword semantics beyond platform-specific count/length limits.

## Proposed Design

### Core Model

```kotlin
data class TerracottaProject(
    // ... existing fields ...
    val keywords: List<String> = emptyList(),
)
```

Keywords are ordered but the order is not semantically significant. Duplicates are normalized during diffing.

### Operation

Add a new operation alongside `UpdateTags`:

```kotlin
data class UpdateKeywords(
    val oldKeywords: List<String>,
    val newKeywords: List<String>,
) : Operation {
    override val description: String =
        "~ Update keywords (from: ${oldKeywords.joinToString()} to: ${newKeywords.joinToString()})"
}
```

`DiffEngine` will produce this operation when `local.keywords` differs from `remote.keywords`.

### Hangar Provider

Hangar stores keywords in `ProjectSettings.keywords`. The live response shape is:

```json
{
  "name": "...",
  "category": "gameplay",
  "description": "...",
  "settings": {
    "keywords": ["clock", "time-display", "translations", "dimension-aware"],
    "tags": ["SUPPORTS_FOLIA"],
    "license": { "name": "MIT", "type": "MIT", "url": "..." },
    "links": [...]
  }
}
```

The provider needs to:

1. Read `settings.keywords` into `TerracottaProject.keywords`.
2. Write keywords via `POST /api/v1/project/{slugOrId}/settings` with the `ProjectSettingsForm` shape:

```json
{
  "settings": {
    "keywords": ["clock", "time-display"],
    "tags": [...],
    "license": {...},
    "links": [...]
  },
  "category": "gameplay",
  "description": "A short description"
}
```

This likely requires moving Hangar metadata updates from the current `PATCH /api/v1/projects/{slug}` call to the supported settings endpoint, and reading the nested `settings` object instead of the flattened fields the current model expects.

### GitHub Provider

Map `UpdateKeywords` to `PUT /repos/{owner}/{repo}/topics`:

```json
{
  "names": ["clock", "time-display", "translations", "dimension-aware"]
}
```

GitHub allows up to 20 topics and each must be 50 characters or fewer. Longer keywords are truncated or dropped; the exact behavior is configurable (see Open Questions).

### Modrinth & CurseForge Providers

Skip `UpdateKeywords` with a warning. Modrinth uses controlled categories; CurseForge uses numeric category IDs. Neither has a keyword field.

## API Sketch

### YAML

```yaml
project:
  name: "ClockTime"
  summary: "A quick-click chat message to clocks with the current in-game time."
  keywords:
    - "clock"
    - "time-display"
    - "translations"
    - "dimension-aware"
```

### Gradle DSL

```kotlin
terracotta {
    project {
        keywords.set(listOf("clock", "time-display", "translations", "dimension-aware"))
    }
}
```

### Operation API

```kotlin
sealed interface Operation {
    data class UpdateKeywords(
        val oldKeywords: List<String>,
        val newKeywords: List<String>,
    ) : Operation
}
```

## Testing Strategy

- **Unit tests**
  - `DiffEngine` produces `UpdateKeywords` when keywords change and produces nothing when they do not.
  - `Operation.UpdateKeywords` description renders correctly.
  - Keyword normalization (trim, dedupe, lowercase) in `DiffEngine`.
- **Provider tests**
  - Hangar: mock `GET /api/v1/projects/{slug}` with nested `settings.keywords`; assert `TerracottaProject.keywords` is parsed.
  - Hangar: mock `POST /api/v1/project/{slugOrId}/settings`; assert the body contains `settings.keywords` and preserves existing `tags`/`license`/`links`/`category`/`description`.
  - GitHub: mock `PUT /repos/{owner}/{repo}/topics`; assert `names` matches keywords.
  - Modrinth/CurseForge: assert `UpdateKeywords` is skipped with a warning.
- **Integration / manual**
  - Run against a real Hangar project to verify keywords appear in the settings UI.
  - Run against a real GitHub repository to verify topics update.

## Documentation Updates

- Add `keywords` to the external YAML configuration reference.
- Add `keywords` to the Gradle DSL reference.
- Update the Hangar provider tutorial to explain keyword limits (max 5).
- Update the GitHub provider tutorial to explain topic mapping (max 20, 50 chars each).
- Update `docs/content/integration/tutorials/publishing-to-multiple-providers.md` with a keyword example.

## Open Questions

1. Should Hangar keyword validation enforce the max 5 limit client-side, or let the API reject it? Enforcing early gives clearer errors.
2. How should GitHub topic length limits be handled? Options: truncate to 50 chars, drop invalid keywords, or fail the operation.
3. Should keywords be normalized (lowercase, no spaces) for GitHub topics while preserving the original strings for Hangar?
4. Does the Hangar provider need to migrate all metadata updates to `POST /api/v1/project/{slugOrId}/settings` in this proposal, or should that be a separate prerequisite change?

## Risks

- **Hangar endpoint/model mismatch**: The current `HangarClient` reads metadata from a flattened shape and writes via `PATCH /api/v1/projects/{slug}`. Adding keywords requires aligning with the actual nested `settings` response and the `POST /api/v1/project/{slugOrId}/settings` endpoint. This may turn into a larger refactor than expected.
  - **Mitigation**: Scope the Hangar changes to a single PR and add targeted tests before touching other providers.

- **Keyword limit overflow**: Hangar allows 5 keywords; GitHub allows 20. A project with more than 5 keywords will fail on Hangar unless truncated or rejected.
  - **Mitigation**: Add provider-specific validation with clear error messages. Make truncation behavior explicit in the provider configuration.

- **Overlap with Narrow Tags**: If [Narrow Tags](./2025-07-narrow-tags.md) lands first, the keyword field must remain distinct from the new `categories` field. If this lands first, Narrow Tags must not remove the free-form concept that keywords later rely on.
  - **Mitigation**: Cross-reference both proposals and keep the data model separate.
