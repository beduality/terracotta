package io.github.beduality.terracotta.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical project visibility.
 *
 * Maps to provider-specific visibility or status concepts. Providers that do not
 * support visibility changes ignore the corresponding operation with a warning.
 *
 * - Modrinth: maps to the project `status` field (`approved`, `unlisted`, `archived`, `private`, `draft`).
 *
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
@Serializable
enum class TerracottaVisibility(val id: String) {
    /** Publicly listed and discoverable. */
    @SerialName("public")
    PUBLIC("public"),

    /** Accessible by direct URL but hidden from listings and search. */
    @SerialName("unlisted")
    UNLISTED("unlisted"),

    /** Read-only and marked as no longer maintained. */
    @SerialName("archived")
    ARCHIVED("archived"),

    /** Visible only to project members or the owner. */
    @SerialName("private")
    PRIVATE("private"),

    /** Not yet published; visible only to the owner. */
    @SerialName("draft")
    DRAFT("draft"),
    ;

    override fun toString(): String = id

    companion object {
        private val byId = entries.associateBy { it.id }

        /**
         * Parses a visibility from its [id], case-insensitive.
         *
         * @throws IllegalArgumentException if [id] is not a supported visibility.
         */
        fun fromId(id: String): TerracottaVisibility {
            val normalizedId = id.lowercase()
            return byId[normalizedId]
                ?: throw IllegalArgumentException(
                    "Unsupported visibility '$id'. Supported visibilities: ${entries.joinToString { it.id }}.",
                )
        }
    }
}
