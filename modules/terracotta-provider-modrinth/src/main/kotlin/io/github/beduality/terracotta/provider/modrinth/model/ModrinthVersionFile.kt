package io.github.beduality.terracotta.provider.modrinth.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModrinthVersionFile(
    val url: String,
    val filename: String,
    val primary: Boolean,
)
