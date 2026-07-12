package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.TerracottaLoaderRegistry
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata

/**
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-metadata-detector.html)
 */

class GameVersionMetadataDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val versions =
            TerracottaLoaderRegistry
                .detectAll(context.cache)
                .flatMap { it.detectGameVersions(context.cache) }
                .flatMap { GameVersionNormalizer.normalize(it) }
                .distinct()

        return if (versions.isEmpty()) null else TerracottaProjectMetadata(gameVersions = versions)
    }
}
