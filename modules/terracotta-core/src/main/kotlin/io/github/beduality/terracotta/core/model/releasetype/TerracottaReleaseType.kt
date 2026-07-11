package io.github.beduality.terracotta.core.model.releasetype

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The stability channel for a version upload.
 *
 * All three major registries (Modrinth, Hangar, CurseForge) support these
 * three release types, though the terminology varies slightly:
 * - Modrinth: version_type = "release" | "beta" | "alpha"
 * - Hangar: channel = "Release" | "Snapshot" (alpha/beta typically mapped to Snapshot)
 * - CurseForge: releaseType = "release" | "beta" | "alpha"
 */
@Serializable
enum class TerracottaReleaseType(val id: String) {
    @SerialName("release")
    RELEASE("release"),

    @SerialName("beta")
    BETA("beta"),

    @SerialName("alpha")
    ALPHA("alpha"),
    ;

    override fun toString(): String = id

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): TerracottaReleaseType {
            val normalizedId = id.lowercase()
            return byId[normalizedId]
                ?: throw IllegalArgumentException(
                    "Unsupported release type '$id'. Supported types: ${entries.joinToString { it.id }}.",
                )
        }
    }
}
