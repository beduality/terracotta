package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Built-in [TerracottaLoader] for Forge.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/core/explanation/loader-hierarchy.html)
 */

open class ForgeLoader : AbstractTerracottaLoader("forge", "Forge") {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean {
        val content = cache.read("src/main/resources/META-INF/mods.toml") ?: return false
        val modLoader = Regex("""modLoader\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1)
        return modLoader?.lowercase() == "javafml"
    }

    /** Detects GameVersions from project files. */
    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/META-INF/mods.toml") ?: return emptyList()
        val lines = content.lines()
        var insideMinecraftBlock = false
        for (line in lines) {
            if (line.contains("""modId\s*=\s*"minecraft"""".toRegex())) {
                insideMinecraftBlock = true
            }
            if (insideMinecraftBlock) {
                val match = Regex("""versionRange\s*=\s*"([^"]+)"""").find(line)
                if (match != null) {
                    return listOf(match.groupValues[1])
                }
            }
        }
        return emptyList()
    }
}
