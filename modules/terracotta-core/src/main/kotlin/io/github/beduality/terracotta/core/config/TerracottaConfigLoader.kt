package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaDonationLink
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Loads a `terracotta.yml` file into [TerracottaConfig].
 *
 * @see [Load a terracotta.yml file](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/load-terracotta-config.html)
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
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
            categories = parseCategories(map["categories"]),
            license = map.readString("license"),
            licenseUrl = map.readString("licenseUrl"),
            icon = map.readString("icon"),
            gameVersions = map.readStringList("gameVersions"),
            loaders = map.readStringList("loaders"),
            environment = map.readString("environment"),
            releaseType = map.readString("releaseType"),
            visibility = map.readString("visibility"),
            changelog = map.readString("changelog"),
            gallery = parseGallery(map["gallery"]),
            links = parseLinks(map["links"]),
            convention = parseConvention(conventionMap),
            providers = parseProviders(providersMap),
        )
    }

    private fun parseCategories(value: Any?): TerracottaProjectCategories? {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?> ?: return null

        @Suppress("UNCHECKED_CAST")
        val primaryMap = map["primary"] as? Map<String, Any?> ?: return null
        val primary = parseCategory(primaryMap) ?: return null

        @Suppress("UNCHECKED_CAST")
        val additionalList = map["additional"] as? List<Map<String, Any?>> ?: emptyList()

        return TerracottaProjectCategories(
            primary = primary,
            additional = additionalList.mapNotNull { parseCategory(it) },
        )
    }

    private fun parseCategory(map: Map<String, Any?>): TerracottaCategory? {
        val id = map.readString("id") ?: return null
        val displayName = map.readString("displayName") ?: id
        return TerracottaCategory(id, displayName)
    }

    private fun parseGallery(value: Any?): List<TerracottaGalleryItem>? {
        @Suppress("UNCHECKED_CAST")
        val list = value as? List<Map<String, Any?>> ?: return null

        return list.map { map ->
            TerracottaGalleryItem(
                imagePath = map.readString("path") ?: "",
                title = map.readString("title") ?: "",
                description = map.readString("description") ?: "",
                featured = map.readBoolean("featured") ?: false,
                ordering = map.readInt("ordering") ?: 0,
            )
        }
    }

    private fun Map<String, Any?>.readBoolean(key: String): Boolean? {
        return when (val value = this[key]) {
            is Boolean -> value
            else -> null
        }
    }

    private fun Map<String, Any?>.readInt(key: String): Int? {
        return when (val value = this[key]) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun parseLinks(value: Any?): TerracottaProjectLinks? {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?> ?: return null

        @Suppress("UNCHECKED_CAST")
        val donations =
            (map["donations"] as? List<Map<String, Any?>>)?.mapNotNull { donation ->
                val platform = donation.readString("platform") ?: return@mapNotNull null
                val url = donation.readString("url") ?: return@mapNotNull null
                TerracottaDonationLink(platform, url)
            } ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val other = (map["other"] as? Map<String, Any?>)?.mapValues { it.value.toString() } ?: emptyMap()

        return TerracottaProjectLinks(
            homepage = map.readString("homepage"),
            source = map.readString("source"),
            issues = map.readString("issues"),
            wiki = map.readString("wiki"),
            community = map.readString("community"),
            donations = donations,
            other = other,
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
