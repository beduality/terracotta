package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

open class BungeeCordLoader : AbstractTerracottaLoader("bungeecord", "BungeeCord") {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/bungee.yml") != null
}
