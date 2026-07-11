package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import org.slf4j.LoggerFactory

/**
 * Applies Terracotta operations to Hangar by translating them into Hangar API calls.
 *
 * @see [Hangar provider guide](https://beduality.github.io/terracotta/content/sdk/how-to-guides/hangar-provider.html)
 */
class HangarRegistryProvider(private val client: HangarClient) : RegistryProvider {
    private val logger = LoggerFactory.getLogger(HangarRegistryProvider::class.java)

    /**
     * Applies [operations] to the Hangar project identified by [projectId].
     *
     * @param projectId Hangar project slug or ID.
     * @param operations changes to apply.
     */
    override suspend fun apply(
        projectId: String,
        operations: List<Operation>,
    ) {
        var slug = projectId
        val updateMetadataOps = mutableListOf<Operation.UpdateMetadata>()
        val updateDescriptionOps = mutableListOf<Operation.UpdateDescription>()
        val updateTagsOps = mutableListOf<Operation.UpdateTags>()

        for (operation in operations) {
            when (operation) {
                is Operation.CreateProject -> {
                    logger.warn(
                        "Hangar does not expose a project creation API. " +
                            "Please create project '${operation.project.name}' manually on Hangar first.",
                    )
                }
                is Operation.UpdateMetadata -> updateMetadataOps.add(operation)
                is Operation.UpdateDescription -> updateDescriptionOps.add(operation)
                is Operation.UpdateTags -> updateTagsOps.add(operation)
                is Operation.UploadVersion -> client.uploadVersion(slug, operation.version)
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
