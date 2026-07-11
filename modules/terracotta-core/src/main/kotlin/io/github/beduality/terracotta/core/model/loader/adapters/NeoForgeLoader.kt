package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Built-in [TerracottaLoader] for NeoForge.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/modules/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/loader-hierarchy.html)
 */

class NeoForgeLoader : AbstractTerracottaLoader("neoforge", "NeoForge", ForgeLoader()) {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean {
        val content = cache.read("src/main/resources/META-INF/mods.toml") ?: return false
        val modLoader = Regex("""modLoader\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1)
        return modLoader?.lowercase() in setOf("neoforge", "lowcodefml")
    }
}
