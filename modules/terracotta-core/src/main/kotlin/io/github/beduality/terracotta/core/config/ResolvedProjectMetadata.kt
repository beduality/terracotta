package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

/**
 * Fully resolved project metadata after merging explicit configuration,
 * auto-detected values, and defaults.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 */
data class ResolvedProjectMetadata(
    /** Project display name. */
    val name: String,
    /** Short description or tagline. */
    val summary: String,
    /** Full project description. */
    val description: String,
    /** Search tags. */
    val tags: List<String>,
    /** SPDX license identifier. */
    val license: String,
    /** Optional URL to the full license text. */
    val licenseUrl: String?,
    /** Supported Minecraft game versions. */
    val gameVersions: List<String>,
    /** Supported loader identifiers. */
    val loaders: List<String>,
    /** Runtime environment. */
    val environment: TerracottaEnvironment,
    /** Release type. */
    val releaseType: TerracottaReleaseType,
    /** Release notes for the current version. */
    val changelog: String,
    /** Path to the project icon file, or null if not configured. */
    val icon: String?,
    /** Gallery images for the project. */
    val gallery: List<TerracottaGalleryItem>,
    /** Identifier of the README convention used. */
    val readmeConvention: String,
    /** Identifier of the changelog convention used. */
    val changelogConvention: String,
)
