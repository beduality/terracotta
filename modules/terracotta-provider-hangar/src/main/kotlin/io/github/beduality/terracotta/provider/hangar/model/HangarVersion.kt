package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

@Serializable
data class HangarVersion(
    val version: String,
    val channel: String,
    val description: String? = null,
    val platformDependencies: Map<String, List<String>> = emptyMap(),
    val fileName: String = "",
)
