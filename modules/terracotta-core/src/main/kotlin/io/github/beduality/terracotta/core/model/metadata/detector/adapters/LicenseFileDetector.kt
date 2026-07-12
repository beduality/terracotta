package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata
import io.github.beduality.terracotta.core.model.projectfile.LicenseFile

/**
 * Detects the project license by inspecting `LICENSE` or `LICENSE.txt`.
 *
 * Only a small set of common SPDX identifiers is recognized; unknown licenses
 * are ignored so they do not override explicit configuration.
 *
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Implement a custom metadata detector tutorial](https://beduality.github.io/terracotta/content/modules/core/tutorials/implementing-a-custom-metadata-detector.html)
 */
class LicenseFileDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val license = LicenseFile.load(context.cache).licenseId ?: return null
        return TerracottaProjectMetadata(license = license)
    }
}
