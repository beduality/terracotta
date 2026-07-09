package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersionFile(
    val url: String,
    val filename: String,
    val primary: Boolean,
)
