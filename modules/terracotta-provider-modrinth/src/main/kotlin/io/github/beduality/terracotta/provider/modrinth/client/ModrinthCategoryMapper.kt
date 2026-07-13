package io.github.beduality.terracotta.provider.modrinth.client

import io.github.beduality.terracotta.core.model.TerracottaProjectCategories

/**
 * Modrinth category split.
 *
 * @property featured Featured categories (max 3: primary + up to 2 additional).
 * @property additional Remaining categories sent as additional categories.
 */
internal data class ModrinthCategories(
    val featured: List<String>,
    val additional: List<String>,
)

/**
 * Maps canonical Terracotta categories to Modrinth's featured / additional split.
 *
 * The primary category is always featured, followed by up to two additional
 * categories. Any remaining categories are sent as additional categories.
 */
internal fun TerracottaProjectCategories.toModrinthCategories(): ModrinthCategories {
    val allIds = listOf(primary.id) + additional.map { it.id }
    val featured = allIds.take(3)
    val additional = allIds.drop(3)
    return ModrinthCategories(featured, additional)
}
