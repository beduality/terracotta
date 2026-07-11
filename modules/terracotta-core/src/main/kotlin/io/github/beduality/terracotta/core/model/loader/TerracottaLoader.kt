package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Represents a mod/plugin platform loader.
 *
 * Implementations are registered in [TerracottaLoaderRegistry] and may declare a
 * parent loader to model platform forks (e.g. Paper extends Spigot, which extends
 * Bukkit).
 */
interface TerracottaLoader {
    /** Unique identifier for this loader, used in configuration and APIs. */
    val id: String

    /** Human-readable display name. */
    val displayName: String

    /** Parent loader, if this loader is a fork of another platform. */
    val parent: TerracottaLoader?

    /** Returns `true` when the given project contains this loader's descriptor. */
    fun detect(cache: ProjectFileCache): Boolean

    /**
     * Detects the environment (client/server/universal) for this loader, if the
     * project descriptor carries that information. Returns `null` when unknown.
     */
    fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment?

    /**
     * Detects supported game versions for this loader from its descriptor file(s).
     *
     * Returns an empty list when the descriptor does not carry version
     * information.
     */
    fun detectGameVersions(cache: ProjectFileCache): List<String> = emptyList()

    /** Returns `true` if this loader is [loader] or inherits from it. */
    fun isOrInheritsFrom(loader: TerracottaLoader): Boolean {
        var current: TerracottaLoader? = this
        while (current != null) {
            if (current == loader) return true
            current = current.parent
        }
        return false
    }
}
