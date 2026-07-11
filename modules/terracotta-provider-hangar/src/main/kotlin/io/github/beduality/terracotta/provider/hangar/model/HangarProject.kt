package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

/**
 * Hangar project response.
 */
@Serializable
data class HangarProject(
    /** Project name. */
    val name: String,
    /** Short project summary. */
    val description: String = "",
    /** Full project description. */
    val body: String = "",
    /** Project tags. */
    val tags: List<String> = emptyList(),
    /** License identifier. */
    val license: String? = null,
)
