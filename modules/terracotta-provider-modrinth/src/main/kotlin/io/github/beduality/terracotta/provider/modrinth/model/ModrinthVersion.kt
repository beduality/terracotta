package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modrinth version response.
 */
@Serializable
data class ModrinthVersion(
    /** Modrinth version identifier. */
    val id: String = "",
    /** Version number string. */
    @SerialName("version_number") val versionNumber: String,
    /** Supported Minecraft game versions. */
    @SerialName("game_versions") val gameVersions: List<String>,
    /** Loader identifiers for this version. */
    val loaders: List<String> = emptyList(),
    /** Files associated with this version. */
    val files: List<ModrinthVersionFile>,
    /** Modrinth version type identifier. */
    @SerialName("version_type") val versionType: String = "release",
    /** Release notes. */
    val changelog: String? = null,
)
