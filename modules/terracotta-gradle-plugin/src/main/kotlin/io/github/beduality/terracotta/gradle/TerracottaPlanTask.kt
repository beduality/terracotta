package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.diff.OperationPreprocessor
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.StateProvider
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.ServiceLoader

/**
 * Gradle task that previews changes Terracotta would apply to a remote registry.
 *
 * Outputs a human-readable plan without modifying the remote state.
 *
 * @see [Tasks reference](https://beduality.github.io/terracotta/content/modules/gradle-plugin/reference/tasks.html)
 */
@DisableCachingByDefault(because = "Task makes network calls to fetch remote project state")
abstract class TerracottaPlanTask : DefaultTask() {
    @get:Input
    /** Registry project identifier. */
    abstract val projectId: Property<String>

    @get:Input
    /** Project display name. */
    abstract val modName: Property<String>

    @get:Input
    /** Short project summary. */
    abstract val summary: Property<String>

    @get:Input
    /** Full project description. */
    abstract val modDescription: Property<String>

    @get:Input
    /** Search tags. */
    abstract val tags: ListProperty<String>

    @get:Input
    /** SPDX license identifier. */
    abstract val license: Property<String>

    @get:Input
    /** Supported Minecraft game versions. */
    abstract val gameVersions: ListProperty<String>

    @get:Input
    /** Supported loader identifiers. */
    abstract val loaders: ListProperty<String>

    @get:Input
    /** Runtime environment. */
    abstract val environment: Property<TerracottaEnvironment>

    @get:Input
    /** Release type. */
    abstract val releaseType: Property<TerracottaReleaseType>

    @get:Input
    /** Release notes for the current version. */
    abstract val changelog: Property<String>

    @get:Input
    /** Provider identifier. */
    abstract val provider: Property<String>

    @get:Input
    @get:Optional
    /** Authentication token for the provider registry. */
    abstract val token: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    /** Compiled artifact to upload. */
    abstract val artifactFile: RegularFileProperty

    /** Computes and prints the operations that would be applied. */
    @TaskAction
    fun plan() =
        runBlocking {
            val local = createLocalProject()

            val providerFactory = findProviderFactory(provider.get())
            val stateProvider: StateProvider = providerFactory.createStateProvider(token.orNull)
            val remote = stateProvider.fetchProject(projectId.get())

            val operations = DiffEngine.diff(local, remote)
            val preprocessedOperations = OperationPreprocessor.process(operations)

            logger.lifecycle("Terracotta Plan:")
            if (preprocessedOperations.isEmpty()) {
                logger.lifecycle("  No changes needed!")
            } else {
                preprocessedOperations.forEach { op ->
                    logger.lifecycle("  ${op.description}")
                }
            }
        }

    private fun createLocalProject(): TerracottaProject {
        val version =
            TerracottaVersion(
                version = project.version.toString(),
                artifactPath = artifactFile.get().asFile.absolutePath,
                gameVersions = gameVersions.get(),
                loaders = loaders.get(),
                environment = environment.get(),
                releaseType = releaseType.get(),
                changelog = changelog.get(),
            )

        return TerracottaProject(
            id = projectId.get(),
            name = modName.get(),
            summary = summary.get(),
            description = modDescription.get(),
            versions = listOf(version),
            tags = tags.get(),
            license = license.get(),
        )
    }

    private fun findProviderFactory(id: String): ProviderFactory {
        val loader = ServiceLoader.load(ProviderFactory::class.java)
        return loader.find { it.id == id }
            ?: throw GradleException("No provider found with id '$id'. Make sure the provider is added as a dependency.")
    }
}
