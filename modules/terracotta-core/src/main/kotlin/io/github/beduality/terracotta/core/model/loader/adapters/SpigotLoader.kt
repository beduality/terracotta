package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class SpigotLoader : AbstractTerracottaLoader("spigot", "Spigot", BukkitLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/plugin.yml") != null
}
