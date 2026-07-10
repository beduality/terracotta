package io.github.beduality.terracotta.core.provider

/**
 * A factory that can create state and registry providers for a specific registry.
 * Implementations should be registered as Java services via ServiceLoader.
 */
interface ProviderFactory {
    /** The unique identifier for this registry (e.g. "modrinth") */
    val id: String

    /** Creates a state provider for this registry using the given auth token (may be null) */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry using the given auth token (may be null) */
    fun createRegistryProvider(token: String?): RegistryProvider
}
