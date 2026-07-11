package io.github.beduality.terracotta.core.model.version

/**
 * Resolves game-version conventions from string identifiers.
 *
 * @see [Version conventions reference](https://beduality.github.io/terracotta/content/core/reference/version-conventions.html)
 */
object GameVersionConventionResolver {
    /**
     * Returns the [GameVersionConvention] for the given [id].
     *
     * Defaults to [MinecraftGameVersionConvention] when [id] is null or unknown.
     */
    fun resolve(id: String?): GameVersionConvention =
        when (id?.lowercase()) {
            "minecraft", null -> MinecraftGameVersionConvention
            else -> throw IllegalArgumentException("Unknown game version convention '$id'")
        }
}
