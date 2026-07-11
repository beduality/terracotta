package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

open class ForgeLoader : AbstractTerracottaLoader("forge", "Forge") {
    override fun detect(cache: ProjectFileCache): Boolean {
        val content = cache.read("src/main/resources/META-INF/mods.toml") ?: return false
        val modLoader = Regex("""modLoader\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1)
        return modLoader?.lowercase() == "javafml"
    }

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
