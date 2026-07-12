package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.projectfile.ReadmeFile

/**
 * Detects project summary and description from `README.md`.
 *
 * The full file content becomes the description, and the first non-empty,
 * non-heading paragraph becomes the summary.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-metadata-detector.html)
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
