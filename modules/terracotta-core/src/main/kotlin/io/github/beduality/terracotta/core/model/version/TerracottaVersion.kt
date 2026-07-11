package io.github.beduality.terracotta.core.model.version

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

/**
 * @see [Models reference](https://beduality.github.io/terracotta/content/core/reference/models.html)
 */
data class TerracottaVersion(
    /** Version string. */
    val version: String,
    /** Path to the compiled artifact file. */
    val artifactPath: String,
    /** Supported Minecraft game versions. */
    val gameVersions: List<String>,
    /** Loader identifiers for this version. */
    val loaders: List<String> = emptyList(),
    /** Runtime environment for this version. */
    val environment: TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY,
    /** Release type for this version. */
    val releaseType: TerracottaReleaseType = TerracottaReleaseType.RELEASE,
    /** Release notes for this version. */
    val changelog: String = "",
    /** Human-readable version title. */
    val displayName: String = "",
)
