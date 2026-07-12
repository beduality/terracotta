package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaLoaderRegistry
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Detects project loaders by querying all registered [TerracottaLoader]s.
 *
 * Also infers the environment by asking each detected loader for its environment.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-metadata-detector.html)
 */
class LoaderMetadataDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val loaders = TerracottaLoaderRegistry.detectAll(context.cache)
        if (loaders.isEmpty()) return null

        return TerracottaProjectMetadata(
            loaders = loaders.map { it.id },
            environment = detectEnvironment(context.cache, loaders),
        )
    }

    private fun detectEnvironment(
        cache: ProjectFileCache,
        loaders: List<TerracottaLoader>,
    ): TerracottaEnvironment? =
        loaders
            .asReversed()
            .asSequence()
            .mapNotNull { it.detectEnvironment(cache) }
            .firstOrNull()
}
