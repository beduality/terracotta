package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class FoliaLoader : AbstractTerracottaLoader("folia", "Folia", PaperLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = false
}
