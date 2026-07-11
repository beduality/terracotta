package io.github.beduality.terracotta.core.model.version

/**
 * Strategy for parsing and canonicalizing a game-version string according to a
 * specific convention.
 *
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/core/reference/version-conventions.html)
 * @see [Normalize game versions guide](https://beduality.github.io/terracotta/content/core/how-to-guides/normalize-game-versions.html)
 */
interface GameVersionConvention {
    /**
     * Parses the given [raw] version string and returns a canonical version
     * identifier when it is valid for this convention, or `null` otherwise.
     */
    fun parse(raw: String): String?
}
