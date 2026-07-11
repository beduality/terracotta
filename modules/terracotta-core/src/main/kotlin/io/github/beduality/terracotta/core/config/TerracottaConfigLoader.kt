package io.github.beduality.terracotta.core.config

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Loads a `terracotta.yml` file into [TerracottaConfig].
 */
object TerracottaConfigLoader {
    /**
     * Loads the given [file] as a [TerracottaConfig].
     *
     * Returns an empty [TerracottaConfig] when the file does not exist.
     */
    fun load(file: File): TerracottaConfig {
        if (!file.exists()) {
            return TerracottaConfig()
        }

        @Suppress("UNCHECKED_CAST")
        val map = Yaml().load(file.inputStream()) as? Map<String, Any?> ?: emptyMap()
        return parse(map)
    }

    private fun parse(map: Map<String, Any?>): TerracottaConfig {
        @Suppress("UNCHECKED_CAST")
        val providersMap = map["providers"] as? Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val conventionMap = map["convention"] as? Map<String, Any?>

        return TerracottaConfig(
            name = map.readString("name"),
            summary = map.readString("summary"),
            description = map.readString("description"),
            tags = map.readStringList("tags"),
            license = map.readString("license"),
            gameVersions = map.readStringList("gameVersions"),
            loaders = map.readStringList("loaders"),
            environment = map.readString("environment"),
            releaseType = map.readString("releaseType"),
            changelog = map.readString("changelog"),
            convention = parseConvention(conventionMap),
            providers = parseProviders(providersMap),
        )
    }

    private fun parseConvention(map: Map<String, Any?>?): TerracottaConventionConfig {
        if (map == null) return TerracottaConventionConfig()

        return TerracottaConventionConfig(
            readme = map.readString("readme"),
            changelog = map.readString("changelog"),
        )
    }

    private fun parseProviders(map: Map<String, Any?>?): Map<String, TerracottaProviderConfig> {
        if (map == null) return emptyMap()

        return map.mapValues { (_, value) ->
            @Suppress("UNCHECKED_CAST")
            val providerMap = value as? Map<String, Any?> ?: emptyMap()
            TerracottaProviderConfig(
                projectId = providerMap.readString("projectId"),
                token = providerMap.readString("token"),
            )
        }
    }

    private fun Map<String, Any?>.readString(key: String): String? {
        val value = this[key]
        return value?.toString()
    }

    private fun Map<String, Any?>.readStringList(key: String): List<String>? {
        val value = this[key] ?: return null
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is List<*> -> value.map { it.toString() }
            else -> null
        }
    }
}
