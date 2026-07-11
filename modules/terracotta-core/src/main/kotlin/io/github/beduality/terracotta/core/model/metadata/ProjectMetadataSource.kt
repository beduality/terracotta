package io.github.beduality.terracotta.core.model.metadata

/**
 * Gradle/project-system-agnostic source of project metadata that does not come
 * from files.
 *
 * @see [Resolve project metadata guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/resolve-project-metadata.html)
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 */
data class ProjectMetadataSource(
    /** Project name supplied by the build system. */
    val name: String? = null,
    /** Project summary supplied by the build system. */
    val summary: String? = null,
    /** Project version supplied by the build system. */
    val version: String? = null,
)
