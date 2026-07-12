package io.github.beduality.terracotta.core.asset

import java.util.ServiceLoader

/**
 * Discovers [AssetProcessor] implementations via [ServiceLoader].
 *
 * Returns the first custom processor found on the classpath, or
 * [IdentityAssetProcessor] when none is registered.
 */
object AssetProcessorLoader {
    /** Loads the registered [AssetProcessor] or falls back to [IdentityAssetProcessor]. */
    fun load(): AssetProcessor {
        return ServiceLoader.load(AssetProcessor::class.java).firstOrNull()
            ?: IdentityAssetProcessor
    }
}
