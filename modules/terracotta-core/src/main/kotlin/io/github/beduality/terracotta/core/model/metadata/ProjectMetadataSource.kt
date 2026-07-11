package io.github.beduality.terracotta.core.model.metadata

/**
 * Gradle/project-system-agnostic source of project metadata that does not come
 * from files.
 */
data class ProjectMetadataSource(
    val name: String? = null,
    val summary: String? = null,
    val version: String? = null,
)
