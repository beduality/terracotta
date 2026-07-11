package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Convenience base class for built-in loader implementations.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/modules/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-loader.html)
 */
abstract class AbstractTerracottaLoader(
    /** Unique identifier. */
    override val id: String,
    /** Human-readable loader name. */
    override val displayName: String,
    /** Parent loader, if this loader is a fork. */
    override val parent: TerracottaLoader? = null,
) : TerracottaLoader {
    /** Detects Environment from project files. */
    override fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment? = null

    override fun toString(): String = id

    override fun equals(other: Any?): Boolean = other is TerracottaLoader && other.id == id

    override fun hashCode(): Int = id.hashCode()
}
