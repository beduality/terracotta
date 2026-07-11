package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.model.TerracottaProject

/**
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/core/reference/provider-interfaces.html)
 * @see [Implement a custom provider tutorial](https://beduality.github.io/terracotta/content/core/tutorials/implementing-a-custom-provider.html)
 */

interface StateProvider {
    /**
     * Fetches the current remote state of a project.
     * Returns null if the project does not exist yet.
     */
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
