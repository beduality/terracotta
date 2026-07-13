package io.github.beduality.terracotta.core.model

import kotlinx.serialization.Serializable

/**
 * A provider-agnostic project category.
 *
 * The [id] is the canonical identifier used throughout Terracotta and by the
 * Gradle DSL. Each registry provider maps this identifier to its own platform
 * category or tag vocabulary.
 *
 * @property id Canonical category identifier (e.g. `"adventure"`, `"library"`).
 * @property displayName Human-readable display name for the category.
 *
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
@Serializable
data class TerracottaCategory(
    /** Canonical category identifier. */
    val id: String,
    /** Human-readable display name. */
    val displayName: String,
)
