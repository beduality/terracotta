package io.github.beduality.terracotta.core.state

/**
 * Factory for creating [StateSource] instances.
 *
 * Implementations are discovered through Java's [ServiceLoader] mechanism and
 * selected by their [id]. Each registered factory must provide a unique,
 * stable identifier.
 *
 * @see [StateSource]
 * @see [StateSourceConfig]
 * @see [State Management](https://beduality.github.io/terracotta/content/modules/core/explanation/state-management.html)
 */
interface StateSourceFactory {
    /**
     * Unique identifier for this backend (e.g. `"filesystem"`).
     *
     * This value matches the `stateSource` property in the Gradle DSL.
     */
    val id: String

    /**
     * Creates a [StateSource] configured with the given [config].
     *
     * @param config Backend-specific configuration, including the project
     *   directory and any settings from the DSL.
     * @return A ready-to-use state source.
     * @throws IllegalArgumentException if the configuration is invalid for
     *   this backend.
     */
    fun create(config: StateSourceConfig): StateSource
}
