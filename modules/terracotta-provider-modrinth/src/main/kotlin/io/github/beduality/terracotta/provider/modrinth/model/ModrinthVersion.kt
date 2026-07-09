package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersion(
    @SerialName("version_number") val versionNumber: String,
    @SerialName("game_versions") val gameVersions: List<String>,
    val loaders: List<String> = emptyList(),
    val files: List<ModrinthVersionFile>,
)
