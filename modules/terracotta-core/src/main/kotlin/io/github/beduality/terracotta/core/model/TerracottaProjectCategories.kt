package io.github.beduality.terracotta.core.model

import kotlinx.serialization.Serializable

/**
 * Structured categories for a [TerracottaProject].
 *
 * Maps cleanly to platform-specific category systems:
 *
 * - Modrinth: [primary] + up to two entries from [additional] become featured
 *   categories; remaining entries become additional categories.
 * - Hangar: [primary] maps to the single project category; recognized entries
 *   from [additional] map to Hangar tags.
 * - CurseForge: reserved for future mapping to numeric category IDs.
 *
 * @property primary The main project category. Always required.
 * @property additional Optional supplementary categories.
 *
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
@Serializable
data class TerracottaProjectCategories(
    /** Primary project category. */
    val primary: TerracottaCategory,
    /** Additional project categories. */
    val additional: List<TerracottaCategory> = emptyList(),
)
