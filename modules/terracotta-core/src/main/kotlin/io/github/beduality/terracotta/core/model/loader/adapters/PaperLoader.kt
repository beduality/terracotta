package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Built-in [TerracottaLoader] for Paper.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/core/explanation/loader-hierarchy.html)
 */

open class PaperLoader : AbstractTerracottaLoader("paper", "Paper", SpigotLoader()) {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/paper-plugin.yml") != null

    /** Detects GameVersions from project files. */
    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/paper-plugin.yml") ?: return emptyList()
        val match = Regex("""api-version\s*[:=]\s*['\"]?([^'\"\s]+)""").find(content) ?: return emptyList()
        return listOf(match.groupValues[1])
    }
}
