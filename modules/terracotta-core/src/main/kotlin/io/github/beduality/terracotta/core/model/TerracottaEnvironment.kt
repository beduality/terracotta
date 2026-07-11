package io.github.beduality.terracotta.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @see [Models reference](https://beduality.github.io/terracotta/content/core/reference/models.html)
 */
@Serializable
enum class TerracottaEnvironment(val id: String) {
    /** Runs only on the Minecraft client. */
    @SerialName("client_only")
    CLIENT_ONLY("client_only"),

    /** Runs only on a Minecraft server. */
    @SerialName("server_only")
    SERVER_ONLY("server_only"),

    /** Works on both client and server. */
    @SerialName("universal")
    UNIVERSAL("universal"),
    ;

    override fun toString(): String = id

    companion object {
        private val byId = entries.associateBy { it.id }

        /**
         * Parses an environment from its [id], case-insensitive.
         *
         * @throws IllegalArgumentException if [id] is not a supported environment.
         */
        fun fromId(id: String): TerracottaEnvironment {
            val normalizedId = id.lowercase()
            return byId[normalizedId]
                ?: throw IllegalArgumentException(
                    "Unsupported Terracotta environment '$id'. Supported environments: ${entries.joinToString { it.id }}.",
                )
        }
    }
}
