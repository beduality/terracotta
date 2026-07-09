package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.model.TerracottaProject

interface StateProvider {
    /**
     * Fetches the current remote state of a project.
     * Returns null if the project does not exist yet.
     */
    fun fetchProject(projectId: String): TerracottaProject?
}
