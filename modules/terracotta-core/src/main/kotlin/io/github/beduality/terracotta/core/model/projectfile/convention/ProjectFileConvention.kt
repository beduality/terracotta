package io.github.beduality.terracotta.core.model.projectfile.convention

/**
 * Base type for a project-file convention that can be resolved from a string
 * identifier.
 *
 * Implementations are registered in [ProjectFileConventionRegistry] either
 * programmatically or via Java's [java.util.ServiceLoader] mechanism.
 */
interface ProjectFileConvention {
    /**
     * Returns this convention if it matches the given [id], otherwise `null`.
     */
    fun resolve(id: String): ProjectFileConvention?
}
