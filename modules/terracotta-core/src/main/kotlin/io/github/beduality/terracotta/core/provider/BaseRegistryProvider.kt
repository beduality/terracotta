package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.core.provider.logic.filterAndWarn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Base class for registry providers that own the provider's platform identifier,
 * logger, and skipped-operation filtering.
 *
 * Concrete providers extend this class and implement [applySupported]. The base
 * class automatically filters unsupported operations using the injected
 * [providerLogic] and logs warnings for skipped operations, so providers can
 * focus on translating supported operations into registry-specific API calls.
 *
 * @param providerLogic provider-specific loader mapping and platform behavior.
 * @param platformId unique provider identifier used in warning messages.
 *
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Provider logic explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/provider-logic.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-provider.html)
 * @see [RegistryProvider]
 * @see [ProviderLogic]
 */
abstract class BaseRegistryProvider(
    private val providerLogic: ProviderLogic,
    private val platformId: String,
) : RegistryProvider {
    /**
     * Logger named after the concrete provider class.
     *
     * Subclasses may override this for testing.
     */
    protected open val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * Filters [operations] using the injected platform behavior, logs any skipped
     * operations, and delegates the remaining operations to [applySupported].
     *
     * @param projectId registry-specific project identifier.
     * @param operations changes to apply.
     */
    override suspend fun apply(
        projectId: String,
        operations: List<Operation>,
    ) {
        val filteredOperations =
            providerLogic.platformBehavior.filterAndWarn(operations, logger, platformId)
        applySupported(projectId, filteredOperations)
    }

    /**
     * Applies the supported operations to the remote registry.
     *
     * @param projectId registry-specific project identifier.
     * @param operations operations already filtered by the platform behavior.
     */
    protected abstract suspend fun applySupported(
        projectId: String,
        operations: List<Operation>,
    )
}
