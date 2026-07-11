package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

open class BungeeCordLoader : AbstractTerracottaLoader("bungeecord", "BungeeCord") {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/bungee.yml") != null
}
