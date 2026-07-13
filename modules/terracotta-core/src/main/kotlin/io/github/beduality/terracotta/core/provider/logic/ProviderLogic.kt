package io.github.beduality.terracotta.core.provider.logic

/**
 * Provider-specific logic for transforming canonical Terracotta models and
 * operations into a target registry's platform-specific representation.
 *
 * Implementations are provider-local objects or classes that expose the
 * loader-mapping rules and platform behavior for a single registry.
 *
 * @see [LoaderMapper]
 * @see [PlatformBehavior]
 * @see [ProviderFactory.createProviderLogic]
 */
interface ProviderLogic {
    /** Loader mapping rules for this provider. */
    val loaderMapper: LoaderMapper

    /** Platform behavior rules for this provider, such as stateful updates. */
    val platformBehavior: PlatformBehavior

    /**
     * Returns `true` when this provider can persist a custom license URL.
     *
     * Providers that do not expose a dedicated `licenseUrl` field (such as
     * Hangar) should return `false` so the diff engine ignores the field
     * instead of generating a perpetual metadata update.
     */
    val supportsLicenseUrl: Boolean get() = true
}
