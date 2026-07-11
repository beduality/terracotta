package io.github.beduality.terracotta.core.model.version

import net.swiftzer.semver.SemVer

/**
 * Default version convention based on Semantic Versioning 2.0.0.
 *
 * Tolerates a leading "v" or "V" prefix before delegating to the semver library.
 */
object SemverVersionConvention : VersionConvention {
    override fun parse(version: String): SemVer? {
        val normalized = version.removePrefix("v").removePrefix("V")
        return try {
            SemVer.parse(normalized)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
