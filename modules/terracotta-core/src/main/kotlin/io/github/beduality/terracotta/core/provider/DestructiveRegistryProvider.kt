package io.github.beduality.terracotta.core.provider

/**
 * Optional provider capability for deleting remote projects and their versions.
 *
 * Implementations are provider-specific because registry deletion APIs vary in
 * availability, authentication scope, and semantics.
 *
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-provider.html)
 */
interface DestructiveRegistryProvider {
    /**
     * Deletes the remote project identified by [projectId].
     *
     * This usually removes the project page, all versions, and associated metadata.
     * Providers should make this operation idempotent: if the project does not exist,
     * the call should succeed without throwing.
     */
    suspend fun deleteProject(projectId: String)

    /**
     * Deletes every version of the remote project identified by [projectId],
     * leaving the project page intact.
     *
     * Providers should make this operation idempotent: if the project has no
     * versions, the call should succeed without throwing.
     */
    suspend fun deleteAllVersions(projectId: String)
}
