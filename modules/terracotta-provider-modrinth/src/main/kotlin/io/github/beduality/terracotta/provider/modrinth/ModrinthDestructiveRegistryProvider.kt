package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.provider.DestructiveRegistryProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

/**
 * Deletes Modrinth projects and versions by translating Terracotta destroy calls
 * into Modrinth API requests.
 *
 * @see [Modrinth provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-modrinth/tutorials/using-modrinth.html)
 */
class ModrinthDestructiveRegistryProvider(private val client: ModrinthClient) : DestructiveRegistryProvider {
    /**
     * Deletes the Modrinth project identified by [projectId].
     *
     * @param projectId Modrinth project slug or ID.
     */
    override suspend fun deleteProject(projectId: String) {
        client.deleteProject(projectId)
    }

    /**
     * Deletes every version of the Modrinth project identified by [projectId],
     * leaving the project page itself intact.
     *
     * @param projectId Modrinth project slug or ID.
     */
    override suspend fun deleteAllVersions(projectId: String) {
        val versions = client.getVersions(projectId)
        versions.forEach { version ->
            if (version.id.isNotBlank()) {
                client.deleteVersion(version.id)
            }
        }
    }
}
