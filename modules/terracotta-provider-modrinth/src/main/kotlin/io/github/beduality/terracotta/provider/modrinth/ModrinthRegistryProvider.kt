package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

class ModrinthRegistryProvider(private val client: ModrinthClient) : RegistryProvider {
    override fun apply(
        projectId: String,
        operations: List<Operation>,
    ) {
        operations.forEach { op ->
            when (op) {
                is Operation.UpdateMetadata -> {
                    val patches = mutableMapOf<String, Any>()
                    if (op.nameChanged) patches["title"] = op.newName
                    if (op.summaryChanged) patches["summary"] = op.newSummary
                    if (op.licenseChanged) patches["license_id"] = op.newLicense
                    client.patchProject(projectId, patches)
                }
                is Operation.UpdateDescription -> {
                    client.patchProject(projectId, mapOf("body" to op.newDescription))
                }
                is Operation.UpdateTags -> {
                    client.patchProject(projectId, mapOf("categories" to op.newTags))
                }
                is Operation.UploadVersion -> {
                    client.createVersion(projectId, op.version)
                }
                is Operation.CreateProject -> {
                    client.createProject(op.project)
                }
            }
        }
    }
}
