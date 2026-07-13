package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.state.GalleryItemIdentity

/**
 * Optional provider capability for reporting the stable identities that resulted
 * from applying gallery operations.
 *
 * Implementations choose the cheapest reliable method for the provider: capture
 * identities from upload responses when the API returns them, or re-fetch the
 * remote gallery list afterward when it does not. Returning an empty map is valid
 * for providers that do not support gallery operations.
 *
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-provider.html)
 */
interface GalleryIdentityReporter {
    /**
     * Reports the gallery identities that resulted from applying [operations].
     *
     * The returned map is keyed by the stable local key ([GalleryItemIdentity.localKey])
     * and contains the remote URL and optional provider-specific identifier for each
     * gallery item that was created, updated, or left unchanged. Identities for items
     * that were deleted should be omitted.
     *
     * @param projectId registry-specific project identifier.
     * @param operations operations that were applied to the remote registry.
     * @return map of local keys to gallery identities; may be empty.
     */
    suspend fun reportGalleryIdentities(
        projectId: String,
        operations: List<Operation>,
    ): Map<String, GalleryItemIdentity>
}
