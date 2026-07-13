# Operations

`Operation` is a sealed interface that describes a single change to apply to a remote registry. The diff engine produces these operations; registry providers consume them.

## Operation types

| Type | Description |
|------|-------------|
| `CreateProject(project)` | Create a new remote project and upload all versions. |
| `UpdateMetadata(...)` | Update name, summary, or license. |
| `UpdateDescription(old, new)` | Replace the project description. |
| `UpdateCategories(oldCategories, newCategories)` | Replace the project categories. |
| `UploadVersion(version)` | Upload a new version artifact. |
| `UploadGalleryItem(item)` | Upload a new gallery image. |
| `UpdateGalleryItem(oldItem, newItem)` | Update metadata of an existing gallery image. |
| `DeleteGalleryItem(item)` | Remove a gallery image from the remote project. |
| `UploadIcon(iconPath)` | Upload a project icon. |
| `UpdateIcon(oldIconUrl, iconPath)` | Replace the project icon. |
| `DeleteIcon(iconUrl)` | Delete the project icon. |

## Diff behavior

| Scenario | Operations produced |
|----------|---------------------|
| Remote project is `null` | `CreateProject` + one `UploadVersion` per local version. |
| Name changed | `UpdateMetadata` with `nameChanged = true`. |
| Summary changed | `UpdateMetadata` with `summaryChanged = true`. |
| License changed | `UpdateMetadata` with `licenseChanged = true`. |
| Description changed | `UpdateDescription`. |
| Categories changed | `UpdateCategories`. |
| Local version not present remotely | `UploadVersion`. |
| Local gallery item not present remotely | `UploadGalleryItem`. |
| Matched gallery item metadata changed | `UpdateGalleryItem`. |
| Remote gallery item not present locally | `DeleteGalleryItem`. |
| Local icon configured but remote has none | `UploadIcon`. |
| Local icon differs from remote icon | `UpdateIcon`. |
| Remote icon present but local has none | `DeleteIcon`. |

## License comparison

Licenses are compared case-insensitively to avoid false positives from capitalization differences.

## Preprocessing

`OperationPreprocessor.process` normalizes versions before upload:

- Empty changelogs are replaced with `"Uploaded via Terracotta."`.
- Empty display names are replaced with `"Version ${version}"`.

## See also

- [Compute a Diff](../how-to-guides/compute-a-diff.md)
- [Diff Engine Explanation](../explanation/diff-engine.md)
- [API Documentation](api.md)
