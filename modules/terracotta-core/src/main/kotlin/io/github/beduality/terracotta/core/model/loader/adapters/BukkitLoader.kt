package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

open class BukkitLoader : AbstractTerracottaLoader("bukkit", "Bukkit") {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/plugin.yml") != null

    override fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY
}
