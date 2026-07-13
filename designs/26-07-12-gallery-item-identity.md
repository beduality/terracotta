# Proposal: Stable Gallery Item Identity via Persisted State

**Date**: 2026-07-12  
**Status**: Draft  
**Related**: TODO item "Stabilize gallery item identity via persisted state", `terracotta-core` diff engine, `terracotta-state-filesystem`

## Summary

Use the persisted `TerracottaState` to store stable identities for gallery items. This lets Terracotta match local gallery images with remote ones even when titles, descriptions, or ordering change, avoiding accidental delete-and-reupload cycles that lose provider-side metadata (e.g., URLs, IDs, and view counts).

## Problem Statement

Today `DiffEngine.diff` matches gallery items only by normalized title (or ordering as a fallback). If a user renames a gallery image, the next `terracottaApply` sees the old title as a deletion and the new title as a fresh upload. The provider-side URL and ID are lost, which can break external links and reset platform metrics.

The state model already has the necessary pieces:

- `ProviderState.gallery: Map<String, GalleryItemIdentity>`
- `GalleryItemIdentity(localKey, remoteUrl, remoteId)`
- `YamlStateCodec` encodes/decodes these fields.

But the diff engine and Gradle tasks never load or write this state, so the persisted identities are unused.

## Goals

- Match local gallery items to remote ones using a stable local identity that survives title changes.
- Keep the existing behavior when no persisted identity is available (fallback to title/ordering).
- Update persisted identities after successful uploads, updates, and deletes.
- Avoid requiring users to add new identifiers to their configuration unless they choose to.
- Keep the change backward-compatible for existing providers and DSLs.

## Non-Goals

- Re-architect the general state backend; continue using the existing `StateSource` SPI.
- Introduce cloud state for gallery items; this remains local/CI persisted state.
- Change how gallery items are uploaded beyond recording the resulting identity.
- Add a general operation-result reporting framework beyond gallery identities; this proposal keeps reporting gallery-specific.

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     TerracottaApplyTask                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐ │
│  │ Load state │  │ DiffEngine │  │ RegistryProvider       │ │
│  │            │  │            │  │ + GalleryIdentityReporter │ │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────────────────┘ │
│        │               │               │                    │
│        ▼               ▼               ▼                    │
│  ProviderState.gallery   Operations     Gallery identities    │
│        │               │               │                     │
│        └───────────────┴───────────────┘                     │
│                      │                                        │
│                      ▼                                        │
│              Save updated state                               │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. Stable local identity (`localKey`)

Each gallery item gets a stable local key derived from configuration:

- If the user provides an explicit `key`, use it.
- Otherwise, fall back to the absolute local `imagePath`.

```kotlin
// In terracotta-core
internal fun galleryLocalKey(item: TerracottaGalleryItem): String =
    item.key ?: item.imagePath
```

The Gradle plugin already resolves gallery files to absolute paths (`TerracottaTaskRegistrar.galleryItemsProvider`), so the path-based key is deterministic for a given project layout. If a user moves the file, it is treated as a new item, which matches the mental model that the local file itself changed. The optional `key` lets users override the path when they need a stable identity across file moves or when multiple items share the same image.

### 2. DiffEngine with persisted state

Add an overload that accepts the previously persisted gallery identities:

```kotlin
object DiffEngine {
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
        persistedGallery: Map<String, GalleryItemIdentity> = emptyMap(),
    ): List<Operation>
}
```

Matching algorithm:

1. For each local item, compute `localKey = key ?: imagePath`.
2. Look up `GalleryItemIdentity` in `persistedGallery` by `localKey`.
3. If an identity exists and its `remoteUrl` matches a remote item, treat them as the same item and emit `UpdateGalleryItem` if metadata changed.
4. If no identity exists, fall back to the existing title/ordering match.
5. Remote items not matched by either mechanism are deleted.
6. Local items not matched are uploaded.

This keeps the default behavior unchanged when `persistedGallery` is empty.

### 3. Gallery identity reporting

Keep the existing `RegistryProvider` contract unchanged. Add a separate, optional interface that providers can implement when they support gallery identity reporting:

```kotlin
interface GalleryIdentityReporter {
    /**
     * Reports the gallery identities that resulted from applying [operations].
     *
     * Implementations may capture identities during the apply call or re-fetch
     * the remote gallery afterward. Returning an empty map is valid for providers
     * that do not support gallery operations.
     */
    suspend fun reportGalleryIdentities(
        projectId: String,
        operations: List<Operation>,
    ): Map<String, GalleryItemIdentity>
}
```

The Gradle task calls `registryProvider.apply(...)` as before, then checks whether the provider is also a `GalleryIdentityReporter` and merges the reported identities into state. This keeps the base contract stable and makes reporting opt-in.

`ModrinthRegistryProvider` implements both `BaseRegistryProvider` and `GalleryIdentityReporter`. It captures the remote URL returned by the Modrinth gallery POST for each upload and maps it to the corresponding `localKey`. No remote re-fetch is required for the MVP.

### 4. State update in `TerracottaApplyTask`

`TerracottaApplyTask` becomes the orchestrator:

1. Load `TerracottaState` from `StateSource`.
2. Read or create the `ProviderState` for the current provider.
3. Pass `providerState.gallery` to `DiffEngine.diff`.
4. Apply the operations through `RegistryProvider` as before.
5. If the provider implements `GalleryIdentityReporter`, call `reportGalleryIdentities` and merge the result into `providerState.gallery`:
   - Add new identities from uploads.
   - Update identities whose remote URL changed (e.g., delete-and-reupload semantics).
   - Remove identities for deleted items.
6. Save the updated `TerracottaState`.

`TerracottaPlanTask` also loads state for accurate planning, but it does not save because no changes are made.

### 5. State model changes

No changes to `TerracottaState`, `ProviderState`, or `GalleryItemIdentity` are required. `YamlStateCodec` already supports them. The only additions are:

- The `DiffEngine` overload.
- The optional `key` property on `TerracottaGalleryItem` and the Gradle gallery extension.
- The `GalleryIdentityReporter` interface in `terracotta-core`.
- Provider-specific reporting in `terracotta-provider-modrinth` (and later `terracotta-provider-hangar` when it gains gallery support).

## Module Boundaries and Dependency Rules

- `terracotta-core` owns the diff engine, `Operation`, `GalleryIdentityReporter`, and `GalleryItemIdentity`.
- `terracotta-state-filesystem` owns encoding/decoding; it does not know about the diff engine.
- `terracotta-gradle-plugin` owns loading/saving state and wiring it into tasks.
- Provider modules own the platform-specific mapping from local keys to remote identities.
- No provider module depends on `terracotta-state-filesystem`; state stays a core abstraction.

## Error Handling

- If the state file is corrupt or missing, treat it as an empty state and fall back to title/ordering matching. The task logs a warning but continues.
- If a provider reports an identity for a `localKey` that the task does not recognize, it is ignored.
- If two or more local gallery items resolve to the same `localKey`, the task logs a warning and those items fall back to title/ordering matching. This prevents one identity from silently overwriting another.
- If saving state fails after a successful apply, the task fails fast so the next run does not re-upload images that already succeeded. The error message points to the state file path.

## Configuration and Initialization

No new required user-facing configuration is introduced. Existing `stateSource` and `stateSourceSettings` fields continue to control where state is stored. The feature activates automatically when a state backend is configured.

Optionally, gallery items may declare an explicit `key` in `terracotta.yml` and the Gradle DSL:

```yaml
gallery:
  - path: images/overview.png
    key: overview
    title: Overview
```

If `key` is omitted, the absolute path is used as the stable identity.

## Open Questions and Risks

1. **Hangar gallery support**: Hangar currently does not support gallery operations. This proposal leaves Hangar without a `GalleryIdentityReporter` implementation; it will gain one when gallery support is added.
2. **Remote state refresh**: The MVP relies on providers reporting identities from upload responses. If a provider cannot reliably report identities, we can add a re-fetch fallback later, but that costs an extra API call.
3. **Content hashing**: A content hash could be used as a fallback disambiguator when keys conflict, but it changes identity when the file changes and does not distinguish items with the same image but different metadata. Path/key + warning/fallback is chosen for simplicity.
