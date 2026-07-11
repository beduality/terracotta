package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

/**
 * Default project metadata implementation used by built-in detectors.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/core/reference/metadata-resolution.html)
 */
class TerracottaProjectMetadata(
    name: String? = null,
    summary: String? = null,
    description: String? = null,
    license: String? = null,
    gameVersions: List<String>? = null,
    loaders: List<String>? = null,
    environment: TerracottaEnvironment? = null,
    releaseType: TerracottaReleaseType? = null,
) : AbstractProjectMetadata(
        name = name,
        summary = summary,
        description = description,
        license = license,
        gameVersions = gameVersions,
        loaders = loaders,
        environment = environment,
        releaseType = releaseType,
    ) {
    /** Merges this metadata with [other], preferring local values. */
    override fun merge(other: ProjectMetadata): TerracottaProjectMetadata = super.merge(other) as TerracottaProjectMetadata
}
