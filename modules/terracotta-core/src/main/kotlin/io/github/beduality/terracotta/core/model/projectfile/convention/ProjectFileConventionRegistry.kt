package io.github.beduality.terracotta.core.model.projectfile.convention

import java.util.ServiceLoader

/**
 * Registry of [ProjectFileConvention] implementations.
 *
 * Built-in conventions are registered automatically. Additional conventions
 * can be contributed by other libraries via Java's [ServiceLoader] mechanism by
 * providing a `META-INF/services/io.github.beduality.terracotta.core.model.projectfile.convention.ProjectFileConvention`
 * service file, or by calling [register] at runtime.
 */
object ProjectFileConventionRegistry {
    private val conventions = mutableListOf<ProjectFileConvention>()
    private var loaded = false

    /**
     * Registers the built-in conventions and loads any additional conventions
     * from the classpath via [ServiceLoader].
     *
     * Calling this method multiple times is safe; additional calls are no-ops.
     */
    @Synchronized
    fun load() {
        if (loaded) return
        register(KeepAChangelogConvention)
        register(TerracottaReadmeConvention)
        ServiceLoader.load(ProjectFileConvention::class.java).forEach(::register)
        loaded = true
    }

    /**
     * Registers a convention at runtime.
     *
     * Must be called before [resolve] is used unless the convention is already
     * loaded via [load].
     */
    fun register(convention: ProjectFileConvention) {
        conventions.add(convention)
    }

    /**
     * Resolves a convention for the given [id].
     *
     * [load] must be called before this method is invoked.
     *
     * @throws IllegalArgumentException if no registered convention matches [id].
     */
    fun resolve(id: String): ProjectFileConvention =
        conventions
            .firstNotNullOfOrNull { it.resolve(id) }
            ?: throw IllegalArgumentException("Unknown project-file convention '$id'")
}
