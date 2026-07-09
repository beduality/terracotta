package io.github.beduality.terracotta.provider.modrinth.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    val summary: String,
    val body: String,
    val categories: List<String>,
    val license: ModrinthLicense,
)
