package io.github.beduality.terracotta.core.state

import java.io.File

/**
 * Configuration passed to a [StateSourceFactory] when creating a [StateSource].
 *
 * Backends receive the Gradle project directory (so they can compute default
 * paths) and a map of backend-specific settings supplied through the DSL.
 *
 * @property projectDir The project directory used for default path resolution.
 * @property settings Backend-specific key-value settings.
 * @see [State Management](https://beduality.github.io/terracotta/content/modules/core/explanation/state-management.html)
 */
data class StateSourceConfig(
    val projectDir: File,
    val settings: Map<String, String>,
)
