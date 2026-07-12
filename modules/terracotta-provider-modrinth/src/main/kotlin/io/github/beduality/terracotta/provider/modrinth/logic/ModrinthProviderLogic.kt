package io.github.beduality.terracotta.provider.modrinth.logic

import io.github.beduality.terracotta.core.provider.logic.LoaderMapper
import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic

/**
 * Provider-specific logic for Modrinth.
 *
 * Uses an identity loader mapper and a stateful platform behavior.
 *
 * @see [ModrinthLoaderMapper]
 * @see [ModrinthPlatformBehavior]
 */
object ModrinthProviderLogic : ProviderLogic {
    override val loaderMapper: LoaderMapper = ModrinthLoaderMapper
    override val platformBehavior: PlatformBehavior = ModrinthPlatformBehavior
}
