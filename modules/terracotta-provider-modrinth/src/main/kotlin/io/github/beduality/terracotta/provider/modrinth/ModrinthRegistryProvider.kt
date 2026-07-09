package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

class ModrinthRegistryProvider(private val client: ModrinthClient) : RegistryProvider {
    override suspend fun apply(
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
                    client.patchProject(resolvedProjectId, patches)
                }
                is Operation.UpdateDescription -> {
                    client.patchProject(resolvedProjectId, mapOf("body" to op.newDescription))
                }
                is Operation.UpdateTags -> {
                    client.patchProject(resolvedProjectId, mapOf("categories" to op.newTags))
                }
                is Operation.UploadVersion -> {
                    client.createVersion(resolvedProjectId, op.version)
                }
                is Operation.CreateProject -> {
                    resolvedProjectId = client.createProject(op.project)
                }
            }
        }
    }
}
