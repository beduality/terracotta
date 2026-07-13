package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.diff.galleryLocalKey
import io.github.beduality.terracotta.core.provider.BaseRegistryProvider
import io.github.beduality.terracotta.core.provider.GalleryIdentityReporter
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.core.state.GalleryItemIdentity
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.client.toModrinthCategories
import io.github.beduality.terracotta.provider.modrinth.client.toModrinthStatus

/**
 * Applies Terracotta operations to Modrinth by translating them into Modrinth API calls.
 *
 * Unsupported operations are filtered out by the base class before the provider
 * processes them.
 *
 * @see [Modrinth provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-modrinth/tutorials/using-modrinth.html)
 */
class ModrinthRegistryProvider(
    private val client: ModrinthClient,
    providerLogic: ProviderLogic,
) : BaseRegistryProvider(providerLogic, "modrinth"),
    GalleryIdentityReporter {
    /**
     * Maps local gallery keys to the remote URLs returned by upload operations.
     *
     * Populated during [applySupported] and consumed by [reportGalleryIdentities].
     * Cleared at the start of each apply so a single provider instance is safe
     * across multiple projects.
     */
    private val uploadedGalleryUrls = mutableMapOf<String, String>()

    /**
     * Applies the supported [operations] to the Modrinth project identified by
     * [projectId].
     *
     * @param projectId Modrinth project slug or ID.
     * @param operations changes to apply.
     */
    override suspend fun applySupported(
        projectId: String,
        operations: List<Operation>,
    ) {
        uploadedGalleryUrls.clear()

        // May be updated to the real base62 ID after project creation
        var resolvedProjectId = projectId

        operations.forEach { op ->
            when (op) {
                is Operation.UpdateMetadata -> {
                    val patches = mutableMapOf<String, Any>()
                    if (op.nameChanged) patches["title"] = op.newName
                    if (op.summaryChanged) patches["description"] = op.newSummary
                    if (op.licenseChanged) patches["license_id"] = op.newLicense
                    val newLicenseUrl = op.newLicenseUrl
                    if (op.licenseUrlChanged && newLicenseUrl != null) patches["license_url"] = newLicenseUrl
                    if (op.linksChanged) {
                        val links = op.newLinks
                        links.issues?.let { patches["issues_url"] = it }
                        links.source?.let { patches["source_url"] = it }
                        links.wiki?.let { patches["wiki_url"] = it }
                        links.community?.let { patches["discord_url"] = it }
                        if (links.donations.isNotEmpty()) {
                            patches["donation_urls"] =
                                links.donations.map { donation ->
                                    mapOf(
                                        "id" to donation.platform,
                                        "platform" to donation.platform,
                                        "url" to donation.url,
                                    )
                                }
                        }
                    }
                    client.patchProject(resolvedProjectId, patches)
                }
                is Operation.UpdateDescription -> {
                    client.patchProject(resolvedProjectId, mapOf("body" to op.newDescription))
                }
                is Operation.UpdateCategories -> {
                    val modrinthCategories = op.newCategories.toModrinthCategories()
                    val patches = mutableMapOf<String, Any>()
                    patches["categories"] = modrinthCategories.featured
                    if (modrinthCategories.additional.isNotEmpty()) {
                        patches["additional_categories"] = modrinthCategories.additional
                    }
                    client.patchProject(resolvedProjectId, patches)
                }
                is Operation.UpdateVisibility -> {
                    client.patchProject(resolvedProjectId, mapOf("status" to op.newVisibility.toModrinthStatus()))
                }
                is Operation.UploadVersion -> {
                    client.createVersion(resolvedProjectId, op.version)
                }
                is Operation.CreateProject -> {
                    resolvedProjectId = client.createProject(op.project)
                }
                is Operation.UploadGalleryItem -> {
                    val url = client.uploadGalleryItem(resolvedProjectId, op.item)
                    uploadedGalleryUrls[galleryLocalKey(op.item)] = url
                }
                is Operation.UpdateGalleryItem -> {
                    client.updateGalleryItem(resolvedProjectId, op.oldItem.imagePath, op.newItem)
                }
                is Operation.DeleteGalleryItem -> {
                    client.deleteGalleryItem(resolvedProjectId, op.item.imagePath)
                }
                is Operation.UploadIcon -> {
                    client.uploadIcon(resolvedProjectId, op.iconPath)
                }
                is Operation.UpdateIcon -> {
                    client.uploadIcon(resolvedProjectId, op.iconPath)
                }
                is Operation.DeleteIcon -> {
                    logger.warn("Modrinth does not support deleting a project icon; skipping icon deletion.")
                }
            }
        }
    }

    /**
     * Reports the gallery identities that resulted from applying [operations].
     *
     * Upload URLs are captured during [applySupported]. For update operations the
     * remote URL is the previous item's [TerracottaGalleryItem.imagePath], since
     * Modrinth's gallery patch does not change the image URL.
     *
     * @param projectId Modrinth project slug or ID.
     * @param operations changes that were applied.
     * @return map of local keys to gallery identities.
     */
    override suspend fun reportGalleryIdentities(
        projectId: String,
        operations: List<Operation>,
    ): Map<String, GalleryItemIdentity> {
        val identities = mutableMapOf<String, GalleryItemIdentity>()
        operations.forEach { op ->
            when (op) {
                is Operation.UploadGalleryItem -> {
                    val localKey = galleryLocalKey(op.item)
                    val url = uploadedGalleryUrls[localKey] ?: return@forEach
                    identities[localKey] = GalleryItemIdentity(localKey = localKey, remoteUrl = url)
                }
                is Operation.UpdateGalleryItem -> {
                    val localKey = galleryLocalKey(op.newItem)
                    identities[localKey] = GalleryItemIdentity(localKey = localKey, remoteUrl = op.oldItem.imagePath)
                }
                is Operation.DeleteGalleryItem -> {
                    identities.remove(galleryLocalKey(op.item))
                }
                else -> {
                    // Not a gallery operation; ignore.
                }
            }
        }
        return identities
    }
}
