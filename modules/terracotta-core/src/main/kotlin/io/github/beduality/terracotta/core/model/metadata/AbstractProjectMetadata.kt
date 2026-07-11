package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import java.util.Objects

/**
 * Convenience base class for project metadata implementations.
 *
 * Provides a default [merge] implementation and value-based equality.
 */
abstract class AbstractProjectMetadata(
    override val name: String? = null,
    override val summary: String? = null,
    override val description: String? = null,
    override val license: String? = null,
    override val gameVersions: List<String>? = null,
    override val loaders: List<String>? = null,
    override val environment: TerracottaEnvironment? = null,
    override val releaseType: TerracottaReleaseType? = null,
) : ProjectMetadata {
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
