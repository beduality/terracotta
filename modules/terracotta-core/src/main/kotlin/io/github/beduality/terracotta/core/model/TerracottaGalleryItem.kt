package io.github.beduality.terracotta.core.model

/**
 * A single gallery image for a project.
 *
 * @property imagePath Local file path when declared in config, or remote URL when
 *   fetched from a provider.
 * @property title Human-readable title used as the stable identity key.
 * @property description Optional longer description.
 * @property featured Whether the image should be highlighted by the provider.
 * @property ordering Display order; lower values come first.
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
data class TerracottaGalleryItem(
    /** Local file path or remote URL. */
    val imagePath: String,
    /** Human-readable title used as identity key. */
    val title: String = "",
    /** Optional longer description. */
    val description: String = "",
    /** Whether the image is featured. */
    val featured: Boolean = false,
    /** Display order. */
    val ordering: Int = 0,
)
