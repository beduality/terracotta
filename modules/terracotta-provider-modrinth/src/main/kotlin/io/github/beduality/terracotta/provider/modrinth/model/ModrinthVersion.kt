package io.github.beduality.terracotta.provider.modrinth.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModrinthVersion(
    @JsonProperty("version_number") val versionNumber: String,
    @JsonProperty("game_versions") val gameVersions: List<String>,
    val loaders: List<String> = emptyList(),
    val files: List<ModrinthVersionFile>,
)
