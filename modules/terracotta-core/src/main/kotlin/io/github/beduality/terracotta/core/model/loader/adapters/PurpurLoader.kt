package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class PurpurLoader : AbstractTerracottaLoader("purpur", "Purpur", PaperLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = false
}
