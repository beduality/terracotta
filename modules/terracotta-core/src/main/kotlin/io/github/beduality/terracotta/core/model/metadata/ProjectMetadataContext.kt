package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.KeepAChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.ReadmeConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.TerracottaReadmeConvention

/**
 * Context passed to [ProjectMetadataDetector]s while loading metadata.
 *
 * Holds the file cache, the explicit source metadata, and the conventions used
 * to interpret project files.
 *
 * @see [Resolve project metadata guide](https://beduality.github.io/terracotta/content/core/how-to-guides/resolve-project-metadata.html)
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/core/reference/metadata-resolution.html)
 */
data class ProjectMetadataContext(
    /** Cached reads of the project directory. */
    val cache: ProjectFileCache,
    /** Build-system values that do not come from files. */
    val source: ProjectMetadataSource,
    /** Convention used to interpret `README.md`. */
    val readmeConvention: ReadmeConvention = TerracottaReadmeConvention,
    /** Convention used to interpret `CHANGELOG.md`. */
    val changelogConvention: ChangelogConvention = KeepAChangelogConvention,
)
