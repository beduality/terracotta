package io.github.beduality.terracotta.core.model.version

/**
 * Resolves version conventions from string identifiers.
 *
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/core/reference/version-conventions.html)
 */
object VersionConventionResolver {
    /**
     * Returns the [VersionConvention] for the given [id].
     *
     * Defaults to [SemverVersionConvention] when [id] is null or unknown.
     */
    fun versionConvention(id: String?): VersionConvention =
        when (id?.lowercase()) {
            "semver", null -> SemverVersionConvention
            else -> throw IllegalArgumentException("Unknown version convention '$id'")
        }
}
