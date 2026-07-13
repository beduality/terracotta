package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.BaseRegistryProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.client.toModrinthCategories

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
) : BaseRegistryProvider(providerLogic, "modrinth") {
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
                is Operation.UploadVersion -> {
                    client.createVersion(resolvedProjectId, op.version)
                }
                is Operation.CreateProject -> {
                    resolvedProjectId = client.createProject(op.project)
                }
                is Operation.UploadGalleryItem -> {
                    client.uploadGalleryItem(resolvedProjectId, op.item)
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
}
