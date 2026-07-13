# Diff Engine

The diff engine compares the desired local state with the actual remote state and produces a minimal, semantic list of operations.

## Why a semantic diff

A naive approach would overwrite every field on every run. That is wasteful, harder to audit, and increases the chance of API errors. By producing discrete operations, Terracotta:

- Shows users exactly what will change.
- Avoids touching fields that are already correct.
- Lets registry providers map each operation to the smallest possible API call.

## Operation categories

Operations fall into two groups:

- **Project-level**: create the project, update metadata, description, or categories.
- **Version-level**: upload a version that does not yet exist remotely.

## Why new projects get separate uploads

When a project does not exist remotely, the engine emits `CreateProject` followed by `UploadVersion` for every local version. This matches registry APIs where project creation and version upload are separate endpoints and the `initial_versions` field is deprecated on some platforms.

## Why versions are never deleted

The engine only uploads missing versions. It does not delete remote versions that are absent locally. Deletion is dangerous and rarely desired; a human should remove versions explicitly through the registry UI.

## Preprocessing

Before upload, `OperationPreprocessor` adds safe defaults:

- Empty changelogs become `"Uploaded via Terracotta."` so the registry has something to display.
- Empty display names become `"Version ${version}"`.

## See also

- [Operations Reference](../reference/operations.md)
- [Compute a Diff](../how-to-guides/compute-a-diff.md)
