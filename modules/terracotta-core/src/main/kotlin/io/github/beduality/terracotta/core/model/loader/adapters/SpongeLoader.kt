package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class SpongeLoader : AbstractTerracottaLoader("sponge", "Sponge") {
    override fun detect(cache: ProjectFileCache): Boolean =
        cache.read("src/main/resources/META-INF/sponge_plugins.json") != null
}
