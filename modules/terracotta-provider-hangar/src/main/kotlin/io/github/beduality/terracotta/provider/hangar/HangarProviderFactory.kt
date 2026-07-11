package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient

class HangarProviderFactory : ProviderFactory {
    override val id: String = "hangar"

    override fun createStateProvider(token: String?): StateProvider {
        return HangarStateProvider(HangarClient(token))
    }

    override fun createRegistryProvider(token: String?): RegistryProvider {
        return HangarRegistryProvider(HangarClient(token))
    }
}
