package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Convenience base class for built-in loader implementations.
 */
abstract class AbstractTerracottaLoader(
    override val id: String,
    override val displayName: String,
    override val parent: TerracottaLoader? = null,
) : TerracottaLoader {
    override fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment? = null

    override fun toString(): String = id

    override fun equals(other: Any?): Boolean = other is TerracottaLoader && other.id == id

    override fun hashCode(): Int = id.hashCode()
}
