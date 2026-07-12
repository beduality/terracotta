package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.state.StateSource
import io.github.beduality.terracotta.core.state.StateSourceConfig
import io.github.beduality.terracotta.core.state.StateSourceFactory
import org.gradle.api.GradleException
import java.io.File
import java.util.ServiceLoader

/**
 * Resolves a [StateSource] from the configured backend identifier and settings.
 */
internal object StateSourceResolver {
    /**
     * Loads the [StateSourceFactory] with the given [id] from the classpath and
     * creates a [StateSource] using the provided [projectDir] and [settings].
     *
     * @throws GradleException if no factory with the given [id] is found.
     */
    fun resolve(
        id: String,
        projectDir: File,
        settings: Map<String, String>,
    ): StateSource {
        val loader = StateSourceFactory::class.java.classLoader
        val factories = ServiceLoader.load(StateSourceFactory::class.java, loader).toList()
        val availableIds = factories.map { it.id }.sorted()
        val factory =
            factories.find { it.id == id }
                ?: throw GradleException(buildMissingBackendMessage(id, availableIds))
        return factory.create(StateSourceConfig(projectDir = projectDir, settings = settings))
    }

    private fun buildMissingBackendMessage(
        id: String,
        availableIds: List<String>,
    ): String {
        val message =
            "No state source factory found with id '$id'. " +
                "Available factories: $availableIds.\n" +
                "Make sure the backend module is on the classpath."
        return if (id == "filesystem") {
            message +
                " For the default filesystem backend, add one of the following:\n" +
                "  Gradle plugin buildscript: classpath(\"io.github.beduality:terracotta-state-filesystem:<version>\")\n" +
                "  Regular dependency: implementation(\"io.github.beduality:terracotta-state-filesystem:<version>\")"
        } else {
            message
        }
    }
}
