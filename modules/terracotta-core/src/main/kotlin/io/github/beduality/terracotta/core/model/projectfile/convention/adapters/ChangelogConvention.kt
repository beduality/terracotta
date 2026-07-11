package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Strategy for extracting a release section from a changelog file.
 *
 * Implementations are resolved from string identifiers via
 * [ProjectFileConventionRegistry].
 */
interface ChangelogConvention : ProjectFileConvention {
    /**
     * Extracts the release notes for the given [version] from [content].
     *
     * Returns `null` if the section is not found or the content is empty.
     */
    fun extractVersionSection(
        content: String,
        version: String,
    ): String?

    override fun resolve(id: String): ChangelogConvention?
}
