package io.github.beduality.terracotta.core.detect

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadata
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext

/**
 * Detects project metadata from the project directory.
 *
 * Implementations may read platform-specific files (such as `fabric.mod.json`
 * or `mods.toml`) and return any values they can infer. They should be
 * registered as Java services via [java.util.ServiceLoader].
 *
 * Detectors receive a [ProjectMetadataContext] so that multiple detectors can
 * share the file cache and the conventions used to interpret project files.
 *
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-metadata-detector.html)
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 */
interface ProjectMetadataDetector {
    /**
     * Detects metadata using the given [context].
     *
     * Returns `null` when the detector cannot contribute any metadata.
     */
    fun detect(context: ProjectMetadataContext): ProjectMetadata?
}
