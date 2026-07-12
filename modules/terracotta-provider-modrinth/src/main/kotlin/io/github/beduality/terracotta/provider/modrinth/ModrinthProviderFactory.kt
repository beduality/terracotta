package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.asset.AssetProcessorLoader
import io.github.beduality.terracotta.core.provider.DestructiveRegistryProvider
import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.logic.ModrinthProviderLogic

/**
 * Provider factory for Modrinth.
 *
 * @see [Modrinth provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-modrinth/tutorials/using-modrinth.html)
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Provider API reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 */
class ModrinthProviderFactory : ProviderFactory {
    /** Provider identifier (`modrinth`). */
    override val id: String = "modrinth"

    private val logic = ModrinthProviderLogic

    /** Creates the Modrinth provider logic. */
    override fun createProviderLogic(): ProviderLogic = logic

    /** Creates a Modrinth state provider backed by [token]. */
    override fun createStateProvider(token: String?): StateProvider {
        return ModrinthStateProvider(ModrinthClient(token, assetProcessor = AssetProcessorLoader.load()))
    }

    /** Creates a Modrinth registry provider backed by [token]. */
    override fun createRegistryProvider(token: String?): RegistryProvider {
        return ModrinthRegistryProvider(ModrinthClient(token, assetProcessor = AssetProcessorLoader.load()), logic)
    }

    /** Creates a Modrinth destructive registry provider backed by [token]. */
    override fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider {
        return ModrinthDestructiveRegistryProvider(ModrinthClient(token, assetProcessor = AssetProcessorLoader.load()))
    }
}
