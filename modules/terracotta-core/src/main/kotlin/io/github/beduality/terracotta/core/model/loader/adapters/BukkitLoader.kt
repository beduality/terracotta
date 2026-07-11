package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Built-in [TerracottaLoader] for Bukkit.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/modules/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/loader-hierarchy.html)
 */

open class BukkitLoader : AbstractTerracottaLoader("bukkit", "Bukkit") {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/plugin.yml") != null

    /** Detects Environment from project files. */
    override fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY

    /** Detects GameVersions from project files. */
    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/plugin.yml") ?: return emptyList()
        return extractApiVersion(content)
    }

    private fun extractApiVersion(content: String): List<String> {
        val match = Regex("""api-version\s*[:=]\s*['\"]?([^'\"\s]+)""").find(content) ?: return emptyList()
        return listOf(match.groupValues[1])
    }
}
