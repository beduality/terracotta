package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

/**
 * Metadata about a project that can be auto-detected or explicitly configured.
 *
 * All fields are optional because different detectors may contribute different
 * pieces of information. Detected values are used as low-priority defaults and
 * can be overridden by explicit configuration.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/core/reference/metadata-resolution.html)
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/core/tutorials/implementing-a-custom-metadata-detector.html)
 */
interface ProjectMetadata {
    /** Project name. */
    val name: String?

    /** Short summary or tagline. */
    val summary: String?

    /** Full description. */
    val description: String?

    /** SPDX license identifier. */
    val license: String?

    /** Supported game versions. */
    val gameVersions: List<String>?

    /** Supported loader identifiers. */
    val loaders: List<String>?

    /** Runtime environment (client/server/universal). */
    val environment: TerracottaEnvironment?

    /** Release type (alpha/beta/release). */
    val releaseType: TerracottaReleaseType?

    /**
     * Merges this metadata with [other], preferring values from this instance
     * and filling missing values from [other]. List values are combined as a
     * distinct union.
     */
    fun merge(other: ProjectMetadata): ProjectMetadata
}
