package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

/**
 * Fully resolved project metadata after merging explicit configuration,
 * auto-detected values, and defaults.
 */
data class ResolvedProjectMetadata(
    val name: String,
    val summary: String,
    val description: String,
    val tags: List<String>,
    val license: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val environment: TerracottaEnvironment,
    val releaseType: TerracottaReleaseType,
    val changelog: String,
    val readmeConvention: String,
    val changelogConvention: String,
)
