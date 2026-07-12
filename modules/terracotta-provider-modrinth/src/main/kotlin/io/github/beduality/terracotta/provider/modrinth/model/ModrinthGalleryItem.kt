package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/**
 * Modrinth gallery item response.
 */
@Serializable
data class ModrinthGalleryItem(
    /** URL of the gallery image. */
    val url: String,
    /** Whether the image is featured. */
    val featured: Boolean = false,
    /** Title of the image. */
    val title: String = "",
    /** Description of the image. */
    val description: String = "",
    /** Display order of the image. */
    val ordering: Int = 0,
)
