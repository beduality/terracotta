package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class VelocityLoader : AbstractTerracottaLoader("velocity", "Velocity") {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/velocity-plugin.json") != null
}
