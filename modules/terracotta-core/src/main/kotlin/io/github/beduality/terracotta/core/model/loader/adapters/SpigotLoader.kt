package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Built-in [TerracottaLoader] for Spigot.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/core/explanation/loader-hierarchy.html)
 */

class SpigotLoader : AbstractTerracottaLoader("spigot", "Spigot", BukkitLoader()) {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/plugin.yml") != null
}
