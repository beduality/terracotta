package io.github.beduality.terracotta.core.provider.logic

/**
 * Maps canonical Terracotta loader identifiers to provider-specific platform
 * names.
 *
 * Providers that expose their own loader slugs (e.g. Modrinth) may implement
 * an identity mapper. Providers that group loaders into platform families
 * (e.g. Hangar) return a platform name such as `PAPER` or `VELOCITY`.
 *
 * @see [io.github.beduality.terracotta.core.model.TerracottaLoader]
 * @see [ProviderLogic]
 */
interface LoaderMapper {
    /**
     * Maps a single canonical loader ID to the provider's platform name, or
     * returns `null` when the loader is not supported by this provider.
     *
     * @param loaderId canonical Terracotta loader identifier (e.g. `paper`).
     * @return provider-specific platform name, or `null` if unsupported.
     */
    fun mapToPlatform(loaderId: String): String?

    /**
     * Maps a list of canonical loader IDs to the set of supported provider
     * platforms.
     *
     * Unsupported loaders are silently skipped. Duplicates collapse to a single
     * platform.
     *
     * @param loaderIds canonical Terracotta loader identifiers.
     * @return distinct provider-specific platform names supported by this mapper.
     */
    fun mapToPlatforms(loaderIds: List<String>): Set<String> = loaderIds.mapNotNull { mapToPlatform(it) }.toSet()
}
