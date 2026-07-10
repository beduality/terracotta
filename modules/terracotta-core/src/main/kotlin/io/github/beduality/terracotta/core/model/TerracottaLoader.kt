package io.github.beduality.terracotta.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TerracottaLoader(val id: String) {
    @SerialName("bukkit")
    BUKKIT("bukkit"),

    @SerialName("bungeecord")
    BUNGEECORD("bungeecord"),

    @SerialName("fabric")
    FABRIC("fabric"),

    @SerialName("folia")
    FOLIA("folia"),

    @SerialName("forge")
    FORGE("forge"),

    @SerialName("neoforge")
    NEOFORGE("neoforge"),

    @SerialName("paper")
    PAPER("paper"),

    @SerialName("purpur")
    PURPUR("purpur"),

    @SerialName("quilt")
    QUILT("quilt"),

    @SerialName("spigot")
    SPIGOT("spigot"),

    @SerialName("sponge")
    SPONGE("sponge"),

    @SerialName("velocity")
    VELOCITY("velocity"),

    @SerialName("waterfall")
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
