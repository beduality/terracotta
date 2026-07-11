package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Strategy for extracting project metadata from a README file.
 *
 * Implementations are resolved from string identifiers via
 * [ProjectFileConventionRegistry].
 */
interface ReadmeConvention : ProjectFileConvention {
    /** Extracts the full description from the README [content]. */
    fun extractDescription(content: String): String?

    /** Extracts a short summary from the README [content]. */
    fun extractSummary(content: String): String?

    override fun resolve(id: String): ReadmeConvention?
}
