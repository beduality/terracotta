package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/**
 * Modrinth project response.
 */
@Serializable
data class ModrinthProject(
    /** Modrinth project ID. */
    val id: String,
    /** Project slug used in URLs. */
    val slug: String,
    /** Project title. */
    val title: String,
    /** Short project summary. */
    @kotlinx.serialization.SerialName("description") val summary: String,
    /** Full project description. */
    val body: String,
    /** Featured project categories. */
    val categories: List<String>,
    /** Additional non-featured project categories. */
    @kotlinx.serialization.SerialName("additional_categories")
    val additionalCategories: List<String> = emptyList(),
    /** License information. */
    val license: ModrinthLicense,
    /** Project icon URL. */
    @kotlinx.serialization.SerialName("icon_url") val iconUrl: String? = null,
    /** Gallery images for the project. */
    val gallery: List<ModrinthGalleryItem> = emptyList(),
    /** Issue tracker URL. */
    @kotlinx.serialization.SerialName("issues_url") val issuesUrl: String? = null,
    /** Source code repository URL. */
    @kotlinx.serialization.SerialName("source_url") val sourceUrl: String? = null,
    /** Wiki or documentation URL. */
    @kotlinx.serialization.SerialName("wiki_url") val wikiUrl: String? = null,
    /** Discord invite URL. */
    @kotlinx.serialization.SerialName("discord_url") val discordUrl: String? = null,
    /** Donation platform links. */
    @kotlinx.serialization.SerialName("donation_urls") val donationUrls: List<ModrinthDonationUrl> = emptyList(),
    /** Project status (visibility). */
    val status: String,
)
