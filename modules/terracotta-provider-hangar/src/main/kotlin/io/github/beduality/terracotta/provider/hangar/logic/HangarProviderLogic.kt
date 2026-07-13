package io.github.beduality.terracotta.provider.hangar.logic

import io.github.beduality.terracotta.core.provider.logic.LoaderMapper
import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.provider.hangar.mapper.HangarLoaderMapper

/**
 * Provider-specific logic for Hangar.
 *
 * Reuses the existing [HangarLoaderMapper] for loader-to-platform mapping and
 * exposes a stateful platform behavior that filters unsupported operations.
 *
 * @see [HangarLoaderMapper]
 * @see [HangarPlatformBehavior]
 */
object HangarProviderLogic : ProviderLogic {
    override val loaderMapper: LoaderMapper = HangarLoaderMapper
    override val platformBehavior: PlatformBehavior = HangarPlatformBehavior
    override val supportsLicenseUrl: Boolean = false
}
