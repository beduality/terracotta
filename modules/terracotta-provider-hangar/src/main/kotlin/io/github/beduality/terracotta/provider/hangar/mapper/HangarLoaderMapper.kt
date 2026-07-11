package io.github.beduality.terracotta.provider.hangar.mapper

import org.slf4j.LoggerFactory

object HangarLoaderMapper {
    private val logger = LoggerFactory.getLogger(HangarLoaderMapper::class.java)

    fun mapToPlatforms(loaders: List<String>): Set<String> {
        return loaders.mapNotNull { mapToPlatform(it) }.toSet()
    }

    fun mapToPlatform(loaderId: String): String? {
        return when (loaderId.lowercase()) {
            "bukkit", "folia", "paper", "purpur", "spigot" -> "PAPER"
            "velocity" -> "VELOCITY"
            "bungeecord", "waterfall" -> "WATERFALL"
            else -> {
                logger.warn("Loader '$loaderId' is not supported by Hangar and will be skipped.")
                null
            }
        }
    }

    fun mapPlatformToLoader(platform: String): String? {
        return when (platform.uppercase()) {
            "PAPER" -> "paper"
            "VELOCITY" -> "velocity"
            "WATERFALL" -> "waterfall"
            else -> null
        }
    }
}
