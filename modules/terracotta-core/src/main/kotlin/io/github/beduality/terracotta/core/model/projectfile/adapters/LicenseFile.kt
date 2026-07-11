package io.github.beduality.terracotta.core.model.projectfile

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Represents a `LICENSE` file in the project.
 *
 * Detects a small set of common SPDX identifiers from the file content.
 */
class LicenseFile(content: String?) : AbstractProjectFile(content) {
    companion object {
        private val RELATIVE_PATHS = listOf("LICENSE", "LICENSE.txt", "LICENSE.md")

        /**
         * Loads the license file from the given [cache], trying the candidate
         * paths in order and returning the first one found.
         */
        fun load(cache: ProjectFileCache): LicenseFile =
            LicenseFile(RELATIVE_PATHS.asSequence().mapNotNull { cache.read(it) }.firstOrNull())
    }

    /** Detected SPDX license identifier, or null if unknown. */
    val licenseId: String? =
        content?.let { detectLicenseId(it) }

    private fun detectLicenseId(content: String): String? {
        val upper = content.uppercase()
        return when {
            upper.contains("MIT LICENSE") || upper.contains("THE MIT LICENSE") -> "MIT"
            upper.contains("APACHE LICENSE") && upper.contains("VERSION 2.0") -> "Apache-2.0"
            upper.contains("GNU GENERAL PUBLIC LICENSE") && upper.contains("VERSION 3") -> "GPL-3.0"
            upper.contains("MOZILLA PUBLIC LICENSE") && upper.contains("VERSION 2.0") -> "MPL-2.0"
            upper.contains("BSD 3-CLAUSE") || upper.contains("BSD-3-CLAUSE") -> "BSD-3-Clause"
            upper.contains("BSD 2-CLAUSE") || upper.contains("BSD-2-CLAUSE") -> "BSD-2-Clause"
            upper.contains("UNLICENSE") -> "Unlicense"
            else -> null
        }
    }
}
