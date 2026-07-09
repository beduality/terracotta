package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation

interface RegistryProvider {
    /**
     * Applies the given operations to the remote registry.
     */
    fun apply(
        projectId: String,
        operations: List<Operation>,
    )
}
