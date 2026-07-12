package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.BaseRegistryProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.hangar.client.HangarClient

/**
 * Applies Terracotta operations to Hangar by translating them into Hangar API calls.
 *
 * Unsupported operations are filtered out by the base class before the provider
 * processes them.
 *
 * @see [Hangar provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-hangar/tutorials/using-hangar.html)
 */
class HangarRegistryProvider(
    private val client: HangarClient,
    providerLogic: ProviderLogic,
) : BaseRegistryProvider(providerLogic, "hangar") {
    /**
     * Applies the supported [operations] to the Hangar project identified by
     * [projectId].
     *
     * @param projectId Hangar project slug or ID.
     * @param operations changes to apply.
     */
    override suspend fun applySupported(
        projectId: String,
        operations: List<Operation>,
    ) {
        var slug = projectId
        val updateMetadataOps = mutableListOf<Operation.UpdateMetadata>()
        val updateDescriptionOps = mutableListOf<Operation.UpdateDescription>()
        val updateTagsOps = mutableListOf<Operation.UpdateTags>()

        for (operation in operations) {
            when (operation) {
                is Operation.UpdateMetadata -> {
                    updateMetadataOps.add(operation)
                    if (!operation.newLicenseUrl.isNullOrBlank()) {
                        logger.warn("Hangar does not support licenseUrl; the configured URL will not be published.")
                    }
                }
                is Operation.UpdateDescription -> updateDescriptionOps.add(operation)
                is Operation.UpdateTags -> updateTagsOps.add(operation)
                is Operation.UploadVersion -> client.uploadVersion(slug, operation.version)
                else -> throw UnsupportedOperationException("Unexpected operation for Hangar: ${operation.description}")
            }
        }

        if (updateMetadataOps.isNotEmpty() || updateDescriptionOps.isNotEmpty() || updateTagsOps.isNotEmpty()) {
            applyMetadataUpdates(slug, updateMetadataOps, updateDescriptionOps, updateTagsOps)
        }
    }

    private suspend fun applyMetadataUpdates(
        slug: String,
        metadataOps: List<Operation.UpdateMetadata>,
        descriptionOps: List<Operation.UpdateDescription>,
        tagsOps: List<Operation.UpdateTags>,
    ) {
        val current = client.getProject(slug)

        val name = metadataOps.lastOrNull { it.nameChanged }?.newName ?: current?.name ?: ""
        val summary = metadataOps.lastOrNull { it.summaryChanged }?.newSummary ?: current?.description ?: ""
        val license = metadataOps.lastOrNull { it.licenseChanged }?.newLicense ?: current?.license ?: ""
        val description = descriptionOps.lastOrNull()?.newDescription ?: current?.body ?: ""
        val tags = tagsOps.lastOrNull()?.newTags ?: current?.tags ?: emptyList()

        client.updateProject(
            slug = slug,
            name = name,
            summary = summary,
            description = description,
            license = license,
            tags = tags,
        )
    }
}
