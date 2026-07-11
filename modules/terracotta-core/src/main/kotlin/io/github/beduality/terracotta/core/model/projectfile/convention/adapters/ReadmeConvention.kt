package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Strategy for extracting project metadata from a README file.
 *
 * Implementations are resolved from string identifiers via
 * [ProjectFileConventionRegistry].
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/conventions.html)
 */
interface ReadmeConvention : ProjectFileConvention {
    /** Extracts the full description from the README [content]. */
    fun extractDescription(content: String): String?

    /** Extracts a short summary from the README [content]. */
    fun extractSummary(content: String): String?

    /** Resolves metadata. */
    override fun resolve(id: String): ReadmeConvention?
}
