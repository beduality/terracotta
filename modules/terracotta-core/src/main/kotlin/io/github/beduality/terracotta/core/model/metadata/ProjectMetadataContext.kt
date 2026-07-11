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
 */
data class ProjectMetadataContext(
    val cache: ProjectFileCache,
    val source: ProjectMetadataSource,
    val readmeConvention: ReadmeConvention = TerracottaReadmeConvention,
    val changelogConvention: ChangelogConvention = KeepAChangelogConvention,
)
