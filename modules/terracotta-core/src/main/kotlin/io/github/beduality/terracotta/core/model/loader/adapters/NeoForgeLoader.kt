package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class NeoForgeLoader : AbstractTerracottaLoader("neoforge", "NeoForge", ForgeLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean {
        val content = cache.read("src/main/resources/META-INF/mods.toml") ?: return false
        val modLoader = Regex("""modLoader\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1)
        return modLoader?.lowercase() in setOf("neoforge", "lowcodefml")
    }
}
