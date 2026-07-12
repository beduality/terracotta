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
    /** Project categories / tags. */
    val categories: List<String>,
    /** License information. */
    val license: ModrinthLicense,
    /** Project icon URL. */
    @kotlinx.serialization.SerialName("icon_url") val iconUrl: String? = null,
    /** Gallery images for the project. */
    val gallery: List<ModrinthGalleryItem> = emptyList(),
)
