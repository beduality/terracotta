package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.BaseRegistryProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.hangar.client.HangarCategories
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import io.github.beduality.terracotta.provider.hangar.client.toHangarCategories
import io.github.beduality.terracotta.provider.hangar.mapper.HangarLicenseMapper

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
        val updateCategoriesOps = mutableListOf<Operation.UpdateCategories>()

        for (operation in operations) {
            when (operation) {
                is Operation.UpdateMetadata -> updateMetadataOps.add(operation)
                is Operation.UpdateDescription -> updateDescriptionOps.add(operation)
                is Operation.UpdateCategories -> updateCategoriesOps.add(operation)
                is Operation.UploadVersion -> client.uploadVersion(slug, operation.version)
                else -> throw UnsupportedOperationException("Unexpected operation for Hangar: ${operation.description}")
            }
        }

        if (updateMetadataOps.isNotEmpty() || updateDescriptionOps.isNotEmpty() || updateCategoriesOps.isNotEmpty()) {
            applyMetadataUpdates(slug, updateMetadataOps, updateDescriptionOps, updateCategoriesOps)
        }
    }

    private suspend fun applyMetadataUpdates(
        slug: String,
        metadataOps: List<Operation.UpdateMetadata>,
        descriptionOps: List<Operation.UpdateDescription>,
        categoriesOps: List<Operation.UpdateCategories>,
    ) {
        val current = client.getProject(slug)

        val name = metadataOps.lastOrNull { it.nameChanged }?.newName ?: current?.name ?: ""
        val summary = metadataOps.lastOrNull { it.summaryChanged }?.newSummary ?: current?.description ?: ""
        val license =
            metadataOps.lastOrNull { it.licenseChanged }?.newLicense?.let { HangarLicenseMapper.toHangarLicense(it) }
                ?: current?.license?.let { HangarLicenseMapper.toHangarLicense(it) }
                ?: ""
        val description = descriptionOps.lastOrNull()?.newDescription ?: current?.body ?: ""
        val categories =
            categoriesOps.lastOrNull()?.newCategories?.toHangarCategories()
                ?: HangarCategories(
                    category = current?.category ?: "",
                    tags = current?.tags ?: emptyList(),
                )
        val links = metadataOps.lastOrNull { it.linksChanged }?.newLinks

        client.updateProject(
            slug = slug,
            name = name,
            summary = summary,
            description = description,
            license = license,
            category = categories.category,
            tags = categories.tags,
            links = links,
        )
    }
}
