package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.diff.OperationPreprocessor
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.TerracottaVersion
import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
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

@DisableCachingByDefault(because = "Task makes network calls and applies changes to remote registries")
abstract class TerracottaApplyTask : DefaultTask() {
    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val modName: Property<String>

    @get:Input
    abstract val summary: Property<String>

    @get:Input
    abstract val modDescription: Property<String>

    @get:Input
    abstract val tags: ListProperty<String>

    @get:Input
    abstract val license: Property<String>

    @get:Input
    abstract val gameVersions: ListProperty<String>

    @get:Input
    abstract val loaders: ListProperty<TerracottaLoader>

    @get:Input
    abstract val environment: Property<TerracottaEnvironment>

    @get:Input
    abstract val releaseType: Property<TerracottaReleaseType>

    @get:Input
    abstract val changelog: Property<String>

    @get:Input
    abstract val provider: Property<String>

    @get:Input
    @get:Optional
    abstract val token: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifactFile: RegularFileProperty

    @TaskAction
    fun apply() =
        runBlocking {
            val local = createLocalProject()

            val providerFactory = findProviderFactory(provider.get())
            val stateProvider: StateProvider = providerFactory.createStateProvider(token.orNull)
            val remote = stateProvider.fetchProject(projectId.get())

            val operations = DiffEngine.diff(local, remote)
            val preprocessedOperations = OperationPreprocessor.process(operations)

            logger.lifecycle("Terracotta Apply:")
            if (preprocessedOperations.isEmpty()) {
                logger.lifecycle("  No changes to apply!")
            } else {
                val registryProvider: RegistryProvider = providerFactory.createRegistryProvider(token.orNull)
                preprocessedOperations.forEach { op ->
                    logger.lifecycle("  Applying: ${op.description}")
                }
                registryProvider.apply(projectId.get(), preprocessedOperations)
                logger.lifecycle("  Done!")
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
