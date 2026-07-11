package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class SpigotLoader : AbstractTerracottaLoader("spigot", "Spigot", BukkitLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/plugin.yml") != null
}
