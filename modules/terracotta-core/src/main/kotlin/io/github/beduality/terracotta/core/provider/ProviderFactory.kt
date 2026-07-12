package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.provider.logic.ProviderLogic

/**
 * A factory that can create state and registry providers for a specific registry.
 * Implementations should be registered as Java services via ServiceLoader.
 *
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Provider logic explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/provider-logic.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-provider.html)
 */
interface ProviderFactory {
    /** The unique identifier for this registry (e.g. "modrinth") */
    val id: String

    /** Creates the provider-specific logic for this registry. */
    fun createProviderLogic(): ProviderLogic

    /** Creates a state provider for this registry using the given auth token (may be null) */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry using the given auth token (may be null) */
    fun createRegistryProvider(token: String?): RegistryProvider

    /**
     * Creates a destructive registry provider for this registry using the given auth token (may be null).
     *
     * Returns null if this provider does not support deletion operations.
     */
    fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider? = null
}
