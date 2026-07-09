package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    @kotlinx.serialization.SerialName("description") val summary: String,
    val body: String,
    val categories: List<String>,
    val license: ModrinthLicense,
)
