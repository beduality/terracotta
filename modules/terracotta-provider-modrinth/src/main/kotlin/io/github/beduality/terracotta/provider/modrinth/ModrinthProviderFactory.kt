package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

class ModrinthProviderFactory : ProviderFactory {
    override val id: String = "modrinth"

    override fun createStateProvider(token: String?): StateProvider {
        return ModrinthStateProvider(ModrinthClient(token))
    }

    override fun createRegistryProvider(token: String?): RegistryProvider {
        return ModrinthRegistryProvider(ModrinthClient(token))
    }
}
