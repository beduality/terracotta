package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader

class FoliaLoader : AbstractTerracottaLoader("folia", "Folia", PaperLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = false
}
