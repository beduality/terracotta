package io.github.beduality.terracotta.provider.modrinth.client

import io.github.beduality.terracotta.core.model.TerracottaVisibility

/**
 * Maps canonical Terracotta visibility to Modrinth's project `status` field.
 */
internal fun TerracottaVisibility.toModrinthStatus(): String =
    when (this) {
        TerracottaVisibility.PUBLIC -> "approved"
        TerracottaVisibility.UNLISTED -> "unlisted"
        TerracottaVisibility.ARCHIVED -> "archived"
        TerracottaVisibility.PRIVATE -> "private"
        TerracottaVisibility.DRAFT -> "draft"
    }

/**
 * Maps Modrinth's project `status` to canonical Terracotta visibility.
 *
 * Unrecognized statuses fall back to [TerracottaVisibility.PUBLIC].
 */
internal fun String.toTerracottaVisibility(): TerracottaVisibility =
    when (this) {
        "approved" -> TerracottaVisibility.PUBLIC
        "unlisted" -> TerracottaVisibility.UNLISTED
        "archived" -> TerracottaVisibility.ARCHIVED
        "private" -> TerracottaVisibility.PRIVATE
        "draft" -> TerracottaVisibility.DRAFT
        else -> TerracottaVisibility.PUBLIC
    }
