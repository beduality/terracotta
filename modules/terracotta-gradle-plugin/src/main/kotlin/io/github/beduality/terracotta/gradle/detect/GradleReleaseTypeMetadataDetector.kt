package io.github.beduality.terracotta.gradle.detect

import io.github.beduality.terracotta.core.detect.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.releasetype.detectReleaseType

/**
 * Detects the project release type by inspecting `gradle.properties`.
 *
 * Reads the `version = ...` property and delegates to [detectReleaseType].
 * Unknown or `unspecified` versions are ignored so they do not override
 * explicit configuration.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Release type explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/metadata-resolution.html)
 */
class GradleReleaseTypeMetadataDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val version = extractVersion(context) ?: return null
        val releaseType = detectReleaseType(version) ?: return null
        return TerracottaProjectMetadata(releaseType = releaseType)
    }

    private fun extractVersion(context: ProjectMetadataContext): String? {
        val gradleProperties = context.cache.read("gradle.properties") ?: return null
        val match = VERSION_REGEX.find(gradleProperties)
        return match?.groupValues?.get(1)?.trim()?.takeIf { it != "unspecified" }
    }

    companion object {
        private val VERSION_REGEX = Regex("""^version\s*=\s*([^\s]+)""", RegexOption.MULTILINE)
    }
}
