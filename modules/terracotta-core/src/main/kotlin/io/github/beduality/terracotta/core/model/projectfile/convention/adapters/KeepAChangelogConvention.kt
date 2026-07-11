package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * [Keep a Changelog](https://keepachangelog.com/) convention where each version
 * has its own `## [version]` section.
 */
object KeepAChangelogConvention : ChangelogConvention {
    override fun resolve(id: String): ChangelogConvention? =
        if (id.equals("keep-a-changelog", ignoreCase = true)) this else null

    override fun extractVersionSection(
        content: String,
        version: String,
    ): String? {
        val escapedVersion = Regex.escape(version)
        val pattern = Regex("""## \[$escapedVersion\]\n(.*?)(?=\n## \[|\Z)""", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content) ?: return null
        return match.groupValues[1].trim()
    }
}
