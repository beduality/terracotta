package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class WaterfallLoader : AbstractTerracottaLoader("waterfall", "Waterfall", BungeeCordLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/bungee.yml") != null
}
