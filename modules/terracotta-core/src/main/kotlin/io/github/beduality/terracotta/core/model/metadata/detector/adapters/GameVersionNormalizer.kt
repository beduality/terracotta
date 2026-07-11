package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.model.version.GameVersionConventionResolver

object GameVersionNormalizer {
    private val candidates =
        Regex(
            """\b(\d{2}w\d{2}[a-z])\b|(1\.\d+(?:\.\d+)?(?:-(?:pre|rc)\d+)?)""",
            RegexOption.IGNORE_CASE,
        )

    private val convention = GameVersionConventionResolver.resolve(null)

    fun normalize(raw: String): List<String> =
        candidates
            .findAll(raw)
            .mapNotNull { match ->
                val token = match.groupValues[1].ifEmpty { match.groupValues[2] }
                convention.parse(token)
            }
            .distinct()
            .toList()
}
