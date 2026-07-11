package io.github.beduality.terracotta.core.model.version

import net.swiftzer.semver.SemVer

/**
 * Strategy for parsing and validating version strings according to a specific convention.
 *
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/core/reference/version-conventions.html)
 */
interface VersionConvention {
    /**
     * Parses the given [version] string into a structured [SemVer] value.
     *
     * Returns `null` if the string is not a valid version for this convention.
     */
    fun parse(version: String): SemVer?
}
