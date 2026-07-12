package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject

/**
 * @see [Compute a diff guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/compute-a-diff.html)
 * @see [Operations reference](https://beduality.github.io/terracotta/content/modules/core/reference/operations.html)
 * @see [Diff engine explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/diff-engine.html)
 */
object DiffEngine {
    /**
     * Computes the semantic diff between a desired local project state
     * and the actual remote project state.
     */
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
    ): List<Operation> {
        val operations = mutableListOf<Operation>()

        if (remote == null) {
            // If the project doesn't exist remotely, create it as a draft first,
            // then upload all versions separately (initial_versions is deprecated on the Modrinth API).
            operations.add(Operation.CreateProject(local))
            local.versions.forEach { version ->
                operations.add(Operation.UploadVersion(version))
            }
            return operations
        }

        // Compare basic metadata
        val nameChanged = local.name != remote.name
        val summaryChanged = local.summary != remote.summary
        val licenseChanged = local.license.uppercase() != remote.license.uppercase()
        val licenseUrlChanged = local.licenseUrl != remote.licenseUrl

        if (nameChanged || summaryChanged || licenseChanged || licenseUrlChanged) {
            operations.add(
                Operation.UpdateMetadata(
                    nameChanged = nameChanged,
                    summaryChanged = summaryChanged,
                    licenseChanged = licenseChanged,
                    licenseUrlChanged = licenseUrlChanged,
                    newName = local.name,
                    newSummary = local.summary,
                    newLicense = local.license,
                    newLicenseUrl = local.licenseUrl,
                ),
            )
        }

        // Compare description
        if (local.description != remote.description) {
            operations.add(Operation.UpdateDescription(remote.description, local.description))
        }

        // Compare tags
        val localTags = local.tags.toSet()
        val remoteTags = remote.tags.toSet()
        if (localTags != remoteTags) {
            operations.add(Operation.UpdateTags(remote.tags, local.tags))
        }

        // Compare versions (upload any missing local versions)
        val remoteVersionMap = remote.versions.associateBy { it.version }
        local.versions.forEach { localVersion ->
            val remoteVersion = remoteVersionMap[localVersion.version]
            if (remoteVersion == null) {
                operations.add(Operation.UploadVersion(localVersion))
            }
        }

        // Compare gallery images
        operations.addAll(diffGallery(local.gallery, remote.gallery))

        return operations
    }

    private fun diffGallery(
        local: List<TerracottaGalleryItem>,
        remote: List<TerracottaGalleryItem>,
    ): List<Operation> {
        val operations = mutableListOf<Operation>()

        val remoteByKey = remote.associateBy { galleryKey(it) }
        val localByKey = local.associateBy { galleryKey(it) }

        // Delete remote items not present locally.
        remote.forEach { remoteItem ->
            val key = galleryKey(remoteItem)
            if (key !in localByKey) {
                operations.add(Operation.DeleteGalleryItem(remoteItem))
            }
        }

        // Update or upload local items.
        local.forEach { localItem ->
            val key = galleryKey(localItem)
            val remoteItem = remoteByKey[key]
            if (remoteItem == null) {
                operations.add(Operation.UploadGalleryItem(localItem))
            } else if (hasMetadataChanged(localItem, remoteItem)) {
                operations.add(Operation.UpdateGalleryItem(remoteItem, localItem))
            }
        }

        return operations
    }

    private fun galleryKey(item: TerracottaGalleryItem): String {
        val title = item.title.trim().lowercase()
        return if (title.isNotEmpty()) title else "ordering:${item.ordering}"
    }

    private fun hasMetadataChanged(
        local: TerracottaGalleryItem,
        remote: TerracottaGalleryItem,
    ): Boolean {
        return local.title.trim().lowercase() != remote.title.trim().lowercase() ||
            local.description.trim() != remote.description.trim() ||
            local.featured != remote.featured ||
            local.ordering != remote.ordering
    }
}
