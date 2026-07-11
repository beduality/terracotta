package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation

/**
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/core/reference/provider-interfaces.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/core/tutorials/implementing-a-custom-provider.html)
 */

interface RegistryProvider {
    /**
     * Applies the given operations to the remote registry.
     */
    suspend fun apply(
        projectId: String,
        operations: List<Operation>,
    )
}
