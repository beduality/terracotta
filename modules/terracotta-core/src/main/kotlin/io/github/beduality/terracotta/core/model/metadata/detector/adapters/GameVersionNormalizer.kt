package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.model.version.GameVersionConventionResolver

/**
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/version-conventions.html)
 * @see [Normalize game versions guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/normalize-game-versions.html)
 */

object GameVersionNormalizer {
    private val candidates =
        Regex(
            """\b(\d{2}w\d{2}[a-z])\b|(1\.\d+(?:\.\d+)?(?:-(?:pre|rc)\d+)?)""",
            RegexOption.IGNORE_CASE,
        )

    private val convention = GameVersionConventionResolver.resolve(null)

    /** Normalizes the given version string. */
    fun normalize(raw: String): List<String> =
        candidates
            .findAll(raw)
            .mapNotNull { match ->
                /** Authentication token. */
                val token = match.groupValues[1].ifEmpty { match.groupValues[2] }
                convention.parse(token)
            }
            .distinct()
            .toList()
}
