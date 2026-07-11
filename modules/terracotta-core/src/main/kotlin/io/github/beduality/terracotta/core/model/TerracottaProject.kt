package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.version.TerracottaVersion

/**
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
data class TerracottaProject(
    /** Internal schema version. */
    val schemaVersion: Int = 1,
    /** Registry project identifier. */
    val id: String,
    /** Project display name. */
    val name: String,
    /** Short description or tagline. */
    val summary: String,
    /** Full project description. */
    val description: String,
    /** Released versions of the project. */
    val versions: List<TerracottaVersion>,
    /** Search tags. */
    val tags: List<String>,
    /** SPDX license identifier. */
    val license: String,
)
