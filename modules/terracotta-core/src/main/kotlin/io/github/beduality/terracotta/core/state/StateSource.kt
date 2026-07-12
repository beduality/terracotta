package io.github.beduality.terracotta.core.state

/**
 * Abstract backend for persisting and loading Terracotta run state.
 *
 * Implementations may read from and write to a local file, a CI-provided backend,
 * or a remote service. Callers receive an immutable [TerracottaState] snapshot.
 *
 * @see [State Management](https://beduality.github.io/terracotta/content/modules/core/explanation/state-management.html)
 */
interface StateSource {
    /**
     * Loads the current persisted state.
     *
     * @return the loaded state, or an empty [TerracottaState] if none exists.
     */
    fun load(): TerracottaState

    /**
     * Persists the given state, replacing any previous state.
     *
     * @param state the state to persist.
     */
    fun save(state: TerracottaState)
}
