package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

@Serializable
data class HangarProject(
    val name: String,
    val description: String = "",
    val body: String = "",
    val tags: List<String> = emptyList(),
    val license: String? = null,
)
