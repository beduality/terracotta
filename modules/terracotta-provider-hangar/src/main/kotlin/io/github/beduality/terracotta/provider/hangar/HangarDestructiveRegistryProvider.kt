package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.provider.DestructiveRegistryProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient

/**
 * Deletes Hangar projects and versions by translating Terracotta destroy calls
 * into Hangar API requests.
 *
 * @see [Hangar provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-hangar/tutorials/using-hangar.html)
 */
class HangarDestructiveRegistryProvider(private val client: HangarClient) : DestructiveRegistryProvider {
    /**
     * Deletes the Hangar project identified by [projectId].
     *
     * @param projectId Hangar project slug.
     */
    override suspend fun deleteProject(projectId: String) {
        client.deleteProject(projectId)
    }

    /**
     * Deletes every version of the Hangar project identified by [projectId],
     * leaving the project page itself intact.
     *
     * @param projectId Hangar project slug.
     */
    override suspend fun deleteAllVersions(projectId: String) {
        val versions = client.getVersions(projectId)
        versions.forEach { version ->
            val versionId = version.id.ifBlank { version.version }
            client.deleteVersion(projectId, versionId)
        }
    }
}
