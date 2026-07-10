package io.github.beduality.terracotta.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TerracottaEnvironment(val id: String) {
    @SerialName("client_only")
    CLIENT_ONLY("client_only"),

    @SerialName("server_only")
    SERVER_ONLY("server_only"),

    @SerialName("universal")
    UNIVERSAL("universal"),
    ;

    override fun toString(): String = id

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): TerracottaEnvironment {
            val normalizedId = id.lowercase()
            return byId[normalizedId]
                ?: throw IllegalArgumentException(
                    "Unsupported Terracotta environment '$id'. Supported environments: ${entries.joinToString { it.id }}.",
                )
        }
    }
}
