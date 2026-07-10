package io.github.beduality.terracotta.core.model

enum class TerracottaEnvironment(val id: String) {
    CLIENT_ONLY("client_only"),
    SERVER_ONLY("server_only"),
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
