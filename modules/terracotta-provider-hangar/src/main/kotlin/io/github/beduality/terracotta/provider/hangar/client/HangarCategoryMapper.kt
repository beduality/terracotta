package io.github.beduality.terracotta.provider.hangar.client

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories

/**
 * Hangar category mapping.
 *
 * @property category Single Hangar project category.
 * @property tags Optional Hangar tags recognized from additional categories.
 */
internal data class HangarCategories(
    val category: String,
    val tags: List<String>,
)

/** Hangar tag IDs that can be attached to a project in addition to the single category. */
private val HANGAR_TAGS = setOf("addon", "library", "folia")

/**
 * Maps canonical Terracotta categories to Hangar's single category + tags model.
 *
 * The primary category becomes the Hangar project category. Recognized additional
 * category IDs (`addon`, `library`, `folia`) are mapped to Hangar tags; any other
 * additional categories are dropped.
 */
internal fun TerracottaProjectCategories.toHangarCategories(): HangarCategories {
    val tags = additional.map { it.id }.filter { it in HANGAR_TAGS }
    return HangarCategories(
        category = primary.id,
        tags = tags,
    )
}

/**
 * Maps Hangar's single category and tags back to canonical Terracotta categories.
 */
internal fun HangarCategories.toTerracottaCategories(): TerracottaProjectCategories {
    return TerracottaProjectCategories(
        primary = TerracottaCategory(category, category),
        additional = tags.map { TerracottaCategory(it, it) },
    )
}
