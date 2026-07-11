package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.loader.BukkitLoader
import io.github.beduality.terracotta.core.model.loader.BungeeCordLoader
import io.github.beduality.terracotta.core.model.loader.FabricLoader
import io.github.beduality.terracotta.core.model.loader.FoliaLoader
import io.github.beduality.terracotta.core.model.loader.ForgeLoader
import io.github.beduality.terracotta.core.model.loader.NeoForgeLoader
import io.github.beduality.terracotta.core.model.loader.PaperLoader
import io.github.beduality.terracotta.core.model.loader.PurpurLoader
import io.github.beduality.terracotta.core.model.loader.QuiltLoader
import io.github.beduality.terracotta.core.model.loader.SpigotLoader
import io.github.beduality.terracotta.core.model.loader.SpongeLoader
import io.github.beduality.terracotta.core.model.loader.VelocityLoader
import io.github.beduality.terracotta.core.model.loader.WaterfallLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Dynamic registry for [TerracottaLoader] implementations.
 *
 * Built-in loaders are registered automatically. Additional loaders or forks
 * can be registered at runtime.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/modules/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/loader-hierarchy.html)
 */
object TerracottaLoaderRegistry {
    private val loaders = mutableMapOf<String, TerracottaLoader>()

    init {
        registerDefaults()
    }

    private fun registerDefaults() {
        register(BukkitLoader())
        register(SpigotLoader())
        register(PaperLoader())
        register(FoliaLoader())
        register(PurpurLoader())
        register(FabricLoader())
        register(QuiltLoader())
        register(ForgeLoader())
        register(NeoForgeLoader())
        register(SpongeLoader())
        register(VelocityLoader())
        register(BungeeCordLoader())
        register(WaterfallLoader())
    }

    /**
     * Registers a loader. Replaces any existing loader with the same id.
     *
     * @param loader the loader to register.
     */
    fun register(loader: TerracottaLoader) {
        loaders[loader.id] = loader
    }

    /**
     * Returns the loader with the given [id], or `null` if not registered.
     *
     * @param id the loader identifier, case-insensitive.
     */
    fun findById(id: String): TerracottaLoader? = loaders[id.lowercase()]

    /**
     * Returns the loader with the given [id], throwing if not registered.
     *
     * @param id the loader identifier, case-insensitive.
     * @throws IllegalArgumentException if no loader matches [id].
     */
    fun fromId(id: String): TerracottaLoader =
        findById(id)
            ?: throw IllegalArgumentException(
                "Unsupported Terracotta loader '$id'. Supported loaders: ${all().joinToString { it.id }}.",
            )

    /** Returns all registered loaders in insertion order. */
    fun all(): List<TerracottaLoader> = loaders.values.toList()

    /**
     * Detects all registered loaders for the given [cache].
     *
     * Returns a distinct list of loaders whose [TerracottaLoader.detect] returns
     * `true`. If a child loader is detected, its parent chain is also included.
     *
     * @param cache cached reads of the project directory.
     */
    fun detectAll(cache: ProjectFileCache): List<TerracottaLoader> {
        val detected = all().filter { it.detect(cache) }
        return detected
            .flatMap { loader ->
                generateSequence(loader) { it.parent }.toList()
            }
            .distinctBy { it.id }
    }
}
