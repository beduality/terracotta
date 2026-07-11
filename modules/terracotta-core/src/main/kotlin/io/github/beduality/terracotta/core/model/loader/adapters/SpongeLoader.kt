package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class SpongeLoader : AbstractTerracottaLoader("sponge", "Sponge") {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/META-INF/sponge_plugins.json") != null
}
