package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.detect.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.TerracottaLoaderRegistry
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata

class GameVersionMetadataDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val fromLoaders = TerracottaLoaderRegistry.detectAll(context.cache).flatMap { it.detectGameVersions(context.cache) }
        val fromBuildFiles = BuildFileGameVersionSource.extract(context.cache)

        val versions =
            (fromLoaders + fromBuildFiles)
                .flatMap { GameVersionNormalizer.normalize(it) }
                .distinct()

        return if (versions.isEmpty()) null else TerracottaProjectMetadata(gameVersions = versions)
    }
}
