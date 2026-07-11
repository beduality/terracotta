package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Base type for a project-file convention that can be resolved from a string
 * identifier.
 *
 * Implementations are registered in [ProjectFileConventionRegistry] either
 * programmatically or via Java's [java.util.ServiceLoader] mechanism.
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/conventions.html)
 * @see [Add a project-file convention guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-project-file-convention.html)
 */
interface ProjectFileConvention {
    /**
     * Returns this convention if it matches the given [id], otherwise `null`.
     */
    fun resolve(id: String): ProjectFileConvention?
}
