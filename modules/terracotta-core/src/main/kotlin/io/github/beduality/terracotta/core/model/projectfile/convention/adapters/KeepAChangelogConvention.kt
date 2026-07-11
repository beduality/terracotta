package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * [Keep a Changelog](https://keepachangelog.com/) convention where each version
 * has its own `## [version]` section.
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/core/reference/conventions.html)
 * @see [Add a project-file convention guide](https://beduality.github.io/terracotta/content/core/how-to-guides/add-a-new-project-file-convention.html)
 */
object KeepAChangelogConvention : ChangelogConvention {
    /** Resolves metadata. */
    override fun resolve(id: String): ChangelogConvention? = if (id.equals("keep-a-changelog", ignoreCase = true)) this else null

    /** Extracts release notes for the given [version]. */
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
