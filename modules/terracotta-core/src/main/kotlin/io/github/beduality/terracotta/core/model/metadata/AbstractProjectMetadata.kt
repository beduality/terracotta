package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import java.util.Objects

/**
 * Convenience base class for project metadata implementations.
 *
 * Provides a default [merge] implementation and value-based equality.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/core/reference/metadata-resolution.html)
 */
abstract class AbstractProjectMetadata(
    /** Project display name. */
    override val name: String? = null,
    /** Short description or tagline. */
    override val summary: String? = null,
    /** Full project description. */
    override val description: String? = null,
    /** SPDX license identifier. */
    override val license: String? = null,
    /** Supported Minecraft game versions. */
    override val gameVersions: List<String>? = null,
    /** Supported loader identifiers. */
    override val loaders: List<String>? = null,
    /** Runtime environment. */
    override val environment: TerracottaEnvironment? = null,
    /** Release type. */
    override val releaseType: TerracottaReleaseType? = null,
) : ProjectMetadata {
    /** Merges this metadata with [other], preferring local values. */
    override fun merge(other: ProjectMetadata): ProjectMetadata =
        TerracottaProjectMetadata(
            name = this.name ?: other.name,
            summary = this.summary ?: other.summary,
            description = this.description ?: other.description,
            license = this.license ?: other.license,
            gameVersions = combineLists(this.gameVersions, other.gameVersions),
            loaders = combineLists(this.loaders, other.loaders)?.distinct(),
            environment = this.environment ?: other.environment,
            releaseType = this.releaseType ?: other.releaseType,
        )

    private fun <T> combineLists(
        first: List<T>?,
        second: List<T>?,
    ): List<T>? =
        when {
            first == null -> second
            second == null -> first
            else -> (first + second).distinct()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProjectMetadata) return false
        return name == other.name &&
            summary == other.summary &&
            description == other.description &&
            license == other.license &&
            gameVersions == other.gameVersions &&
            loaders == other.loaders &&
            environment == other.environment &&
            releaseType == other.releaseType
    }

    override fun hashCode(): Int =
        Objects.hash(
            name,
            summary,
            description,
            license,
            gameVersions,
            loaders,
            environment,
            releaseType,
        )

    override fun toString(): String =
        "TerracottaProjectMetadata(" +
            "name=$name, " +
            "summary=$summary, " +
            "description=$description, " +
            "license=$license, " +
            "gameVersions=$gameVersions, " +
            "loaders=$loaders, " +
            "environment=$environment, " +
            "releaseType=$releaseType" +
            ")"
}
