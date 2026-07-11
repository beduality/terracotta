package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

open class PaperLoader : AbstractTerracottaLoader("paper", "Paper", SpigotLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/paper-plugin.yml") != null

    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/paper-plugin.yml") ?: return emptyList()
        val match = Regex("""api-version\s*[:=]\s*['\"]?([^'\"\s]+)""").find(content) ?: return emptyList()
        return listOf(match.groupValues[1])
    }
}
