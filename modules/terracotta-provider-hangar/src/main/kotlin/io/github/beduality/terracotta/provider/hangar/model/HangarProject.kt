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
    /** Project category. */
    val category: String? = null,
    /** Project tags. */
    val tags: List<String> = emptyList(),
    /** License identifier. */
    val license: String? = null,
    /** Project homepage URL. */
    val homepage: String? = null,
    /** Source code repository URL. */
    val source: String? = null,
    /** Issue tracker URL. */
    val issues: String? = null,
    /** Wiki or documentation URL. */
    val wiki: String? = null,
    /** Discord invite URL. */
    val discord: String? = null,
    /** Donation links. */
    val donations: List<HangarDonationLink> = emptyList(),
)
