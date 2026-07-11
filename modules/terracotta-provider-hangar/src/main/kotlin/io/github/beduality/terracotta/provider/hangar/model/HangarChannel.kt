package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

@Serializable
data class HangarChannel(
    val name: String,
    val color: String = "green",
)
