package io.github.beduality.terracotta.core.model

enum class TerracottaLoader(val id: String) {
    BUKKIT("bukkit"),
    BUNGEECORD("bungeecord"),
    FABRIC("fabric"),
    FOLIA("folia"),
    FORGE("forge"),
    NEOFORGE("neoforge"),
    PAPER("paper"),
    PURPUR("purpur"),
    QUILT("quilt"),
    SPIGOT("spigot"),
    VELOCITY("velocity"),
    WATERFALL("waterfall"),
    ;

    override fun toString(): String = id

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): TerracottaLoader {
            val normalizedId = id.lowercase()
            return byId[normalizedId]
                ?: throw IllegalArgumentException(
                    "Unsupported Terracotta loader '$id'. Supported loaders: ${entries.joinToString { it.id }}.",
                )
        }
    }
}
