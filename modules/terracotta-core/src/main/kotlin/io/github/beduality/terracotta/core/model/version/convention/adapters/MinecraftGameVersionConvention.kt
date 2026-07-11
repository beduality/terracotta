package io.github.beduality.terracotta.core.model.version

/**
 * Minecraft game-version convention.
 *
 * Accepts classic releases (`1.20.1`), snapshots (`25w14a`), pre-releases
 * (`1.21.5-pre1`), and release candidates (`1.21.5-rc1`). It tolerates common
 * packaging noise such as dependency operators and brackets.
 *
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/core/reference/version-conventions.html)
 * @see [Normalize game versions guide](https://beduality.github.io/terracotta/content/core/how-to-guides/normalize-game-versions.html)
 */
object MinecraftGameVersionConvention : GameVersionConvention {
    private val classic =
        Regex(
            """^1\.\d+(?:\.\d+)?(?:-(?:pre|rc)\d+)?$""",
            RegexOption.IGNORE_CASE,
        )
    private val snapshot = Regex("""^\d{2}w\d{2}[a-z]$""", RegexOption.IGNORE_CASE)

    /** Parses the given value according to this convention. */
    override fun parse(raw: String): String? {
        val cleaned =
            raw
                .trim()
                .removeSurrounding("[", "]")
                .removeSurrounding("(", ")")
                .replace(Regex("""^[<>=~^]+"""), "")
                .trim()

        val match = classic.find(cleaned) ?: snapshot.find(cleaned)
        return match?.value?.lowercase()
    }
}
