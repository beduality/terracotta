package io.github.beduality.terracotta.provider.hangar.mapper

import org.slf4j.LoggerFactory

/**
 * Maps Terracotta loader identifiers to Hangar platform names and back.
 *
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/core/explanation/loader-hierarchy.html)
 */
object HangarLoaderMapper {
    private val logger = LoggerFactory.getLogger(HangarLoaderMapper::class.java)

    /** Maps a list of loader IDs to supported Hangar platforms. */
    fun mapToPlatforms(loaders: List<String>): Set<String> {
        return loaders.mapNotNull { mapToPlatform(it) }.toSet()
    }

    /** Maps a single loader ID to a Hangar platform, or `null` if unsupported. */
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

    /** Maps a Hangar platform name back to a Terracotta loader ID. */
    fun mapPlatformToLoader(platform: String): String? {
        return when (platform.uppercase()) {
            "PAPER" -> "paper"
            "VELOCITY" -> "velocity"
            "WATERFALL" -> "waterfall"
            else -> null
        }
    }
}
