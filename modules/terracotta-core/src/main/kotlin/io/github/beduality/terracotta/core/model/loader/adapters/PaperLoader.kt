package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

open class PaperLoader : AbstractTerracottaLoader("paper", "Paper", SpigotLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/paper-plugin.yml") != null
}
