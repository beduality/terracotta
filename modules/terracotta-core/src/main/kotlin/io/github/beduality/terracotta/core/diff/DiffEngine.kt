package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.state.GalleryItemIdentity

/**
 * @see [Compute a diff guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/compute-a-diff.html)
 * @see [Operations reference](https://beduality.github.io/terracotta/content/modules/core/reference/operations.html)
 * @see [Diff engine explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/diff-engine.html)
 */
object DiffEngine {
    /**
     * Computes the semantic diff between a desired local project state and the
     * actual remote project state.
     *
     * @param supportsLicenseUrl whether the target provider can persist a custom
     * license URL. When `false`, [licenseUrl] differences are ignored so they
     * do not generate a perpetual metadata update.
     */
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
        supportsLicenseUrl: Boolean = true,
    ): List<Operation> = diff(local, remote, supportsLicenseUrl, emptyMap())

    /**
     * Computes the semantic diff between a desired local project state and the
     * actual remote project state, using previously persisted [persistedGallery]
     * identities to match local gallery items with remote ones across title or
     * ordering changes.
     *
     * @param persistedGallery gallery identities keyed by stable local key.
     */
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
        persistedGallery: Map<String, GalleryItemIdentity>,
    ): List<Operation> = diff(local, remote, true, persistedGallery)

    /**
     * Computes the semantic diff between a desired local project state and the
     * actual remote project state.
     *
     * @param supportsLicenseUrl whether the target provider can persist a custom
     * license URL. When `false`, [licenseUrl] differences are ignored so they
     * do not generate a perpetual metadata update.
     * @param persistedGallery gallery identities keyed by stable local key.
     */
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
        supportsLicenseUrl: Boolean,
        persistedGallery: Map<String, GalleryItemIdentity>,
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
        val licenseUrlChanged = supportsLicenseUrl && local.licenseUrl != remote.licenseUrl
        val linksChanged = local.links != remote.links

        if (nameChanged || summaryChanged || licenseChanged || licenseUrlChanged || linksChanged) {
            operations.add(
                Operation.UpdateMetadata(
                    nameChanged = nameChanged,
                    summaryChanged = summaryChanged,
                    licenseChanged = licenseChanged,
                    licenseUrlChanged = licenseUrlChanged,
                    linksChanged = linksChanged,
                    newName = local.name,
                    newSummary = local.summary,
                    newLicense = local.license,
                    newLicenseUrl = if (supportsLicenseUrl) local.licenseUrl else null,
                    newLinks = local.links,
                ),
            )
        }

        // Compare description
        if (local.description != remote.description) {
            operations.add(Operation.UpdateDescription(remote.description, local.description))
        }

        // Compare categories
        if (local.categories != remote.categories) {
            operations.add(Operation.UpdateCategories(remote.categories, local.categories))
        }

        // Compare visibility
        if (local.visibility != remote.visibility) {
            operations.add(Operation.UpdateVisibility(remote.visibility, local.visibility))
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
        operations.addAll(diffGallery(local.gallery, remote.gallery, persistedGallery))

        // Compare icon
        operations.addAll(diffIcon(local.icon, remote.icon))

        return operations
    }

    private fun diffIcon(
        localIcon: String?,
        remoteIcon: String?,
    ): List<Operation> {
        val operations = mutableListOf<Operation>()

        when {
            localIcon == null && remoteIcon != null -> {
                operations.add(Operation.DeleteIcon(remoteIcon))
            }
            localIcon != null && remoteIcon == null -> {
                operations.add(Operation.UploadIcon(localIcon))
            }
            localIcon != null && remoteIcon != null && localIcon != remoteIcon -> {
                operations.add(Operation.UpdateIcon(remoteIcon, localIcon))
            }
        }

        return operations
    }

    private fun diffGallery(
        local: List<TerracottaGalleryItem>,
        remote: List<TerracottaGalleryItem>,
        persistedGallery: Map<String, GalleryItemIdentity>,
    ): List<Operation> {
        val operations = mutableListOf<Operation>()

        val remoteByUrl = remote.associateBy { it.imagePath }
        val remoteByKey = remote.associateBy { galleryKey(it) }
        val localByKey = local.associateBy { galleryKey(it) }
        val matchedRemote = mutableSetOf<TerracottaGalleryItem>()
        val matchedLocal = mutableSetOf<TerracottaGalleryItem>()

        // Match local items to remote items using persisted identities first.
        local.forEach { localItem ->
            val localKey = galleryLocalKey(localItem)
            val identity = persistedGallery[localKey] ?: return@forEach
            val remoteItem = remoteByUrl[identity.remoteUrl] ?: return@forEach
            matchedLocal.add(localItem)
            matchedRemote.add(remoteItem)
            if (hasMetadataChanged(localItem, remoteItem)) {
                operations.add(Operation.UpdateGalleryItem(remoteItem, localItem))
            }
        }

        // Delete remote items not present locally and not matched by identity.
        remote.forEach { remoteItem ->
            if (remoteItem in matchedRemote) return@forEach
            val key = galleryKey(remoteItem)
            if (key !in localByKey) {
                operations.add(Operation.DeleteGalleryItem(remoteItem))
            }
        }

        // Update or upload local items, skipping identity-matched ones.
        local.forEach { localItem ->
            if (localItem in matchedLocal) return@forEach
            val key = galleryKey(localItem)
            val remoteItem = remoteByKey[key]
            if (remoteItem == null) {
                operations.add(Operation.UploadGalleryItem(localItem))
            } else {
                matchedRemote.add(remoteItem)
                if (hasMetadataChanged(localItem, remoteItem)) {
                    operations.add(Operation.UpdateGalleryItem(remoteItem, localItem))
                }
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

/**
 * Returns the stable local key for [item]: the explicit [TerracottaGalleryItem.key]
 * if present, otherwise the absolute [TerracottaGalleryItem.imagePath].
 */
fun galleryLocalKey(item: TerracottaGalleryItem): String = item.key ?: item.imagePath
