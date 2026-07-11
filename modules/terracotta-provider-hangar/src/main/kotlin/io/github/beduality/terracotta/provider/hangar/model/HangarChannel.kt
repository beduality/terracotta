package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

/**
 * Hangar release channel.
 */
@Serializable
data class HangarChannel(
    /** Channel name. */
    val name: String,
    /** Channel color. */
    val color: String = "green",
)
