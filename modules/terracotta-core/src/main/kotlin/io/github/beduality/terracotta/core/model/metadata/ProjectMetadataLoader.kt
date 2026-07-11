package io.github.beduality.terracotta.core.detect

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.releasetype.detectReleaseType
import java.util.ServiceLoader

/**
 * Loads auto-detected project metadata from all registered
 * [ProjectMetadataDetector]s and from a [ProjectMetadataSource].
 */
object ProjectMetadataLoader {
    fun load(
        cache: ProjectFileCache,
        source: ProjectMetadataSource,
    ): TerracottaProjectMetadata = load(ProjectMetadataContext(cache, source))

    fun load(context: ProjectMetadataContext): TerracottaProjectMetadata {
        val detectedFromFiles = loadFromDetectors(context)
        val detectedFromSource =
            TerracottaProjectMetadata(
                name = context.source.name,
                summary = context.source.summary?.takeIf { it.isNotBlank() },
                releaseType = context.source.version?.let { detectReleaseType(it) },
            )
        return detectedFromSource.merge(detectedFromFiles)
    }

    private fun loadFromDetectors(context: ProjectMetadataContext): TerracottaProjectMetadata {
        val detectors = ServiceLoader.load(ProjectMetadataDetector::class.java)
        var metadata = TerracottaProjectMetadata()
        detectors.forEach { detector ->
            detector.detect(context)?.let { metadata = metadata.merge(it) }
        }
        return metadata
    }
}
