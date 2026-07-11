package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.detect.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.projectfile.ReadmeFile

/**
 * Detects project summary and description from `README.md`.
 *
 * The full file content becomes the description, and the first non-empty,
 * non-heading paragraph becomes the summary.
 */
class ReadmeMetadataDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val file = ReadmeFile.load(context.cache, context.readmeConvention)
        val description = file.description ?: return null

        return TerracottaProjectMetadata(
            description = description,
            summary = file.summary,
        )
    }
}
