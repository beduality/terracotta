---
description: Implementation plan for adding gallery image support, following the module development workflow.
---

# Gallery Assets Implementation Plan

This plan follows the workflow in `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-development-workflow.md` for a **major feature** that touches `terracotta-core`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`, and `terracotta-gradle-plugin`.

## Source of truth

- Design proposal: `@/home/luis/GitHub/beduality/terracotta/project/proposals/2026-07-gallery-assets.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Completed | Proposal reviewed and approved in conversation. |
| TDD | Completed | Tests written and confirmed failing for expected reasons. |
| Implementation | Completed | Core diff, config, validator, asset processor, Modrinth client, Hangar warning, Gradle DSL. |
| Build / quality checks | Completed | All affected modules build, tests pass, spotless green, mkdocs strict build passes. |
| Documentation | Completed | Config schema, operations, models, Modrinth tutorial, Gradle DSL guide, CHANGELOG updated. |
| Review / release prep | Completed | Final review checklist passed. |

## Phase 1: System design

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-system-design-workflow.md`.

### Responsibilities

- `terracotta-core` defines the provider-agnostic gallery model, config schema, diff
  logic, and operation types.
- `terracotta-provider-modrinth` maps the core model to Modrinth API calls.
- `terracotta-provider-hangar` ignores gallery operations with a warning.
- `terracotta-gradle-plugin` exposes gallery configuration in the DSL and passes gallery
  items into tasks.

### Public API sketch

See `@/home/luis/GitHub/beduality/terracotta/project/proposals/2026-07-gallery-assets.md`
for full signatures.

Key additions:

- `TerracottaGalleryItem` in `terracotta-core`.
- `gallery: List<TerracottaGalleryItem>` on `TerracottaProject`.
- `Operation.UploadGalleryItem`, `Operation.UpdateGalleryItem`,
  `Operation.DeleteGalleryItem`.
- `AssetProcessor` / `ProcessedAsset` / `IdentityAssetProcessor` SPI in
  `terracotta-core`.
- `GalleryValidator` in `terracotta-core`.
- `gallery` block in Gradle `TerracottaExtension`.

### Review checklist

Before moving to Phase 2, confirm:

- [ ] Problem and scope are clearly stated.
- [ ] Public API is small and stable.
- [ ] Responsibilities do not overlap between modules.
- [ ] Dependency directions are correct: Gradle plugin depends on core; providers
  implement core operations.
- [ ] Error handling and safety strategy are explicit.
- [ ] Open questions and risks are listed and answered.

## Phase 2: Test-driven development

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-testing-workflow.md`.

### 2.1 Identify behavior

- A gallery item declared in `terracotta.yml` is parsed into `TerracottaConfig`.
- The diff engine emits `UploadGalleryItem` when a local item is not present remotely.
- The diff engine emits `DeleteGalleryItem` when a remote item is not present locally.
  Deletion is enabled by default.
- The diff engine emits `UpdateGalleryItem` when a matched item's metadata changed.
- Items are matched by normalized title, or by ordering when title is empty.
- The core `GalleryValidator` rejects missing files, unsupported extensions, and
  oversized images using provider-specific limits.
- The core `AssetProcessor` SPI passes the original file through unchanged by default.
- The Modrinth client applies the configured `AssetProcessor` before upload.
- The Modrinth client uploads a file with the correct multipart body and query params.
- The Modrinth client updates an existing gallery item via `PATCH` using the remote URL.
- The Modrinth client deletes a gallery item via `DELETE` using the remote URL.
- The Modrinth state provider reads the `gallery` array from a project response.
- The Hangar provider skips gallery operations with a single warning.
- The Gradle plugin registers a `gallery` container and passes items into tasks.

### 2.2 Write failing tests

Add tests under the relevant modules:

- `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/diff/`
  - `DiffEngineGalleryTest`: diff scenarios for upload, update, and delete.
- `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/config/`
  - `TerracottaConfigLoaderTest`: parse `gallery` section from YAML.
- `modules/terracotta-provider-modrinth/src/test/kotlin/io/github/beduality/terracotta/provider/modrinth/`
  - `ModrinthGalleryTest`: client upload/update/delete requests use expected URLs and
    params.
  - `ModrinthStateProviderTest`: gallery items are parsed from project response.
  - `ModrinthRegistryProviderTest`: gallery operations are routed to the right client
    methods.
- `modules/terracotta-provider-hangar/src/test/kotlin/io/github/beduality/terracotta/provider/hangar/`
  - `HangarRegistryProviderTest`: gallery operations are skipped without failing.
- `modules/terracotta-gradle-plugin/src/test/kotlin/io/github/beduality/terracotta/gradle/`
  - `TerracottaPluginGalleryIntegrationTest`: gallery DSL is wired into tasks.

### 2.3 Run tests and confirm they fail

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.diff.*"
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.config.*"
./gradlew :terracotta-provider-modrinth:test --tests "io.github.beduality.terracotta.provider.modrinth.*"
./gradlew :terracotta-provider-hangar:test --tests "io.github.beduality.terracotta.provider.hangar.*"
./gradlew :terracotta-gradle-plugin:test --tests "io.github.beduality.terracotta.gradle.*"
```

## Phase 3: Implementation

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-implementation-workflow.md`.

### 3.1 Core model changes

- Create `TerracottaGalleryItem` in
  `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/model/`.
- Add `gallery: List<TerracottaGalleryItem> = emptyList()` to `TerracottaProject`.

### 3.2 Asset processing SPI

- Create `AssetProcessor`, `ProcessedAsset`, and `IdentityAssetProcessor` in
  `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/asset/`.
- Expose the processor through provider factories or client configuration.

### 3.3 Core validation

- Create `GalleryValidator` in `terracotta-core`.
- Accept provider-specific limits (supported extensions and max size) as parameters.
- Throw `IOException` with a clear message for missing files, unsupported extensions,
  and oversized files.

### 3.4 Config changes

- Add `gallery: List<TerracottaGalleryItem>? = null` to `TerracottaConfig`.
- Update `TerracottaConfigLoader.parse` to read the `gallery` list.
- Parse each map entry: `path`, `title`, `description`, `featured`, `ordering`.

### 3.5 Diff engine changes

- Update `DiffEngine.diff` to compare `local.gallery` and `remote.gallery`.
- Implement identity matching:
  - Normalize title by trimming and lowercasing.
  - If title is empty, use `ordering` as the key.
- Emit operations in this order:
  1. `DeleteGalleryItem` for unmatched remote items.
  2. `UpdateGalleryItem` for matched items with changed metadata.
  3. `UploadGalleryItem` for unmatched local items.

### 3.6 Operation preprocessor changes

- Update `OperationPreprocessor.process` to pass gallery operations through unchanged
  (or normalize empty titles if desired).

### 3.7 Modrinth provider changes

- Add `ModrinthGalleryItem` DTO with `url`, `title`, `description`, `featured`,
  `ordering`, and `created`.
- Add `gallery: List<ModrinthGalleryItem>` to `ModrinthProject`.
- Add client methods:
  - `uploadGalleryItem(projectId, item)` -> `POST /project/{id}/gallery`
  - `updateGalleryItem(projectId, url, item)` -> `PATCH /project/{id}/gallery?url=...`
  - `deleteGalleryItem(projectId, url)` -> `DELETE /project/{id}/gallery?url=...`
- Use the core `GalleryValidator` with Modrinth-specific limits (5 MiB, supported
  extensions).
- Apply the configured `AssetProcessor` to the local image before building the upload
  body.
- Update `ModrinthStateProvider.fetchProject` to map `gallery` to
  `TerracottaGalleryItem`.
- Update `ModrinthRegistryProvider.apply` to handle the three new operations.

### 3.8 Hangar provider changes

- Update `HangarRegistryProvider.apply` to catch gallery operations and emit a warning.

### 3.9 Gradle plugin changes

- Create `TerracottaGalleryExtension` with `imageFile`, `title`, `description`,
  `featured`, and `ordering` properties.
- Add `gallery: NamedDomainObjectContainer<TerracottaGalleryExtension>` to
  `TerracottaExtension`.
- Update `ProjectMetadataResolver` to resolve the gallery list from config into
  `TerracottaProject`.
- Update `TerracottaPlanTask` and `TerracottaApplyTask` to create local projects with
  gallery items.
- Update `TerracottaTaskRegistrar` to wire gallery conventions.

### 3.10 Refactor and stabilize

- Keep gallery logic isolated from version logic.
- Mark internal helpers as `internal`.
- Add KDoc to every new public API element with `@see` links to user docs.

### 3.11 Build and quality checks

```bash
./gradlew :terracotta-core:build :terracotta-core:spotlessCheck
./gradlew :terracotta-provider-modrinth:build :terracotta-provider-modrinth:spotlessCheck
./gradlew :terracotta-provider-hangar:build :terracotta-provider-hangar:spotlessCheck
./gradlew :terracotta-gradle-plugin:build :terracotta-gradle-plugin:spotlessCheck
```

## Phase 4: Build / quality checks

Run the full verification suite for all affected modules:

```bash
./gradlew :terracotta-core:build :terracotta-core:spotlessCheck
./gradlew :terracotta-provider-modrinth:build :terracotta-provider-modrinth:spotlessCheck
./gradlew :terracotta-provider-hangar:build :terracotta-provider-hangar:spotlessCheck
./gradlew :terracotta-gradle-plugin:build :terracotta-gradle-plugin:spotlessCheck
```

## Phase 5: Documentation

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-documentation-workflow.md`.

### 5.1 Core docs

Update `docs/content/modules/core/reference/`:

- `config-schema.md`: add `gallery` top-level field and example.
- `operations.md`: add `UploadGalleryItem`, `UpdateGalleryItem`, `DeleteGalleryItem`.
- `models.md`: add `TerracottaGalleryItem`.

### 5.2 Provider docs

Update `docs/content/modules/provider-modrinth/`:

- `reference/` page: document supported image formats, size limits, and identity
  matching rules.
- `tutorials/using-modrinth.md`: add gallery configuration example.

### 5.3 Gradle plugin docs

Update `docs/content/modules/gradle-plugin/reference/tasks.md` with `gallery` DSL
example.

### 5.4 Verify docs build

```bash
uv run mkdocs build --strict
```

## Phase 6: Review and release preparation

Run the review checkpoints from
`@/home/luis/GitHub/beduality/terracotta/project/methodology/module-review-workflow.md`:

- [ ] Design review completed and open questions resolved.
- [ ] Code review: all behavior covered by tests, no secrets logged, `spotlessCheck`
  green, `:<module>:build` green.
- [ ] Documentation review: Diátaxis structure preserved, `mkdocs build --strict` green.
- [ ] Merge/release review: matches scope guidance, `CHANGELOG.md` updated, CI green.

## Definition of done

- `terracotta.yml` and the Gradle DSL can declare gallery images.
- `terracottaPlan` and `terracottaApply` compute and execute gallery operations.
- Modrinth provider uploads, updates metadata, and deletes gallery images.
- Hangar provider skips gallery operations without failing.
- Tests cover diff, config parsing, client behavior, state mapping, and Gradle wiring.
- Docs build and all affected module builds pass.
- `CHANGELOG.md` is updated under `[Unreleased]`.
