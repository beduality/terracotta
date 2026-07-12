package io.github.beduality.terracotta.provider.hangar.logic

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior

/**
 * Platform behavior for Hangar.
 *
 * Hangar is a stateful registry, but it does not support project creation,
 * gallery images, or project icon operations through its API. Those operations
 * are filtered out before they reach the registry provider.
 */
object HangarPlatformBehavior : PlatformBehavior {
    override val isStateful: Boolean = true

    override fun filterOperations(operations: List<Operation>): List<Operation> =
        operations.filter {
            when (it) {
                is Operation.UpdateMetadata,
                is Operation.UpdateDescription,
                is Operation.UpdateTags,
                is Operation.UploadVersion,
                -> true
                else -> false
            }
        }
}
