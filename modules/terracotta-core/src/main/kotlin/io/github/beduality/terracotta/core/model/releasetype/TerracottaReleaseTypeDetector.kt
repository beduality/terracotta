package io.github.beduality.terracotta.core.model.releasetype

import io.github.beduality.terracotta.core.model.version.SemverVersionConvention

/**
 * Detects the [TerracottaReleaseType] for a version string based on its
 * Semantic Versioning pre-release identifiers.
 *
 * - "alpha" → [TerracottaReleaseType.ALPHA]
 * - "beta", "snapshot", "rc" → [TerracottaReleaseType.BETA]
 * - "unspecified" or unparseable versions → `null`
 * - anything else → [TerracottaReleaseType.RELEASE]
 */
fun detectReleaseType(version: String): TerracottaReleaseType? {
    if (version == "unspecified") return null
    val semVer = SemverVersionConvention.parse(version) ?: return null
    val preRelease = semVer.preRelease?.lowercase() ?: ""
    return when {
        "alpha" in preRelease -> TerracottaReleaseType.ALPHA
        "beta" in preRelease || "snapshot" in preRelease || "rc" in preRelease -> TerracottaReleaseType.BETA
        else -> TerracottaReleaseType.RELEASE
    }
}
