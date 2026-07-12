package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.provider.DestructiveRegistryProvider
import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import io.github.beduality.terracotta.provider.hangar.logic.HangarProviderLogic

/**
 * Provider factory for Hangar.
 *
 * @see [Hangar provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-hangar/tutorials/using-hangar.html)
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 * @see [Provider API reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 */
class HangarProviderFactory : ProviderFactory {
    /** Provider identifier (`hangar`). */
    override val id: String = "hangar"

    private val logic = HangarProviderLogic

    /** Creates the Hangar provider logic. */
    override fun createProviderLogic(): ProviderLogic = logic

    /** Creates a Hangar state provider backed by [token]. */
    override fun createStateProvider(token: String?): StateProvider {
        return HangarStateProvider(HangarClient(token, loaderMapper = logic.loaderMapper))
    }

    /** Creates a Hangar registry provider backed by [token]. */
    override fun createRegistryProvider(token: String?): RegistryProvider {
        return HangarRegistryProvider(HangarClient(token, loaderMapper = logic.loaderMapper), logic)
    }

    /** Creates a Hangar destructive registry provider backed by [token]. */
    override fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider {
        return HangarDestructiveRegistryProvider(HangarClient(token, loaderMapper = logic.loaderMapper))
    }
}
