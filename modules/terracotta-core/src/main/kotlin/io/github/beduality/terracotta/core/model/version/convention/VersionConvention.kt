package io.github.beduality.terracotta.core.model.version

import net.swiftzer.semver.SemVer

/**
 * Strategy for parsing and validating version strings according to a specific convention.
 */
interface VersionConvention {
    /**
     * Parses the given [version] string into a structured [SemVer] value.
     *
     * Returns `null` if the string is not a valid version for this convention.
     */
    fun parse(version: String): SemVer?
}
