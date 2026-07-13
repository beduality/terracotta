package io.github.beduality.terracotta.core.provider.logic

import io.github.beduality.terracotta.core.diff.Operation
import org.slf4j.Logger

/**
 * Describes how a registry behaves when applying Terracotta operations.
 *
 * The platform behavior is used by [io.github.beduality.terracotta.core.provider.RegistryProvider]
 * implementations to decide which operations can be applied. Stateful platforms
 * can update project metadata, while append-only platforms support only file uploads.
 *
 * @see [Operation]
 * @see [ProviderLogic]
 * @see [io.github.beduality.terracotta.core.provider.RegistryProvider]
 */
interface PlatformBehavior {
    /**
     * Returns `true` when the registry supports updating project metadata,
     * descriptions, and categories.
     *
     * Append-only platforms return `false` and implementations should filter
     * metadata operations before applying them.
     */
    val isStateful: Boolean

    /**
     * Filters [operations] according to this platform's capabilities.
     *
     * The default implementation returns the operations unchanged for stateful
     * platforms and keeps only [Operation.UploadVersion] for append-only
     * platforms.
     *
     * @param operations canonical Terracotta operations to apply.
     * @return operations that this platform can actually apply.
     */
    fun filterOperations(operations: List<Operation>): List<Operation> =
        if (isStateful) {
            operations
        } else {
            operations.filterIsInstance<Operation.UploadVersion>()
        }

    /**
     * Splits [operations] into the operations this platform will apply and the
     * operations it will skip.
     *
     * @param operations canonical Terracotta operations to apply.
     * @return pair of `(applied, skipped)` operations.
     */
    fun partition(operations: List<Operation>): Pair<List<Operation>, List<Operation>> {
        val applied = filterOperations(operations)
        val appliedSet = applied.toSet()
        val skipped = operations.filter { it !in appliedSet }
        return applied to skipped
    }
}

/**
 * Filters [operations] according to this platform behavior and logs any skipped
 * operations through [logger] using a generic platform warning.
 *
 * @param operations canonical Terracotta operations to apply.
 * @param logger logger used to warn about skipped operations.
 * @param platformId provider identifier used in warning messages (e.g. "hangar").
 * @return operations that this platform can actually apply.
 */
fun PlatformBehavior.filterAndWarn(
    operations: List<Operation>,
    logger: Logger,
    platformId: String,
): List<Operation> {
    val (applied, skipped) = partition(operations)
    skipped
        .groupBy { it.description }
        .forEach { (description, ops) ->
            logger.warn(
                "Platform '$platformId' does not support operation '$description'; " +
                    "${ops.size} occurrence(s) will be skipped.",
            )
        }
    return applied
}
