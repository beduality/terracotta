package io.github.beduality.terracotta.provider.modrinth.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModrinthLicense(
    val id: String,
)
