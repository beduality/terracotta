package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.diff.OperationPreprocessor
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import io.github.beduality.terracotta.core.model.TerracottaVisibility
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
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

/**
 * Gradle task that applies Terracotta changes to a remote registry.
 *
 * Creates or updates the project metadata and uploads any missing versions.
 *
 * @see [Tasks reference](https://beduality.github.io/terracotta/content/modules/gradle-plugin/reference/tasks.html)
 */
@DisableCachingByDefault(because = "Task makes network calls and applies changes to remote registries")
abstract class TerracottaApplyTask : DefaultTask() {
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
    /** Project categories. */
    abstract val categories: Property<TerracottaProjectCategories>

    @get:Input
    /** SPDX license identifier. */
    abstract val license: Property<String>

    @get:Input
    @get:Optional
    /** Optional URL to the full license text. */
    abstract val licenseUrl: Property<String>

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
    /** Project visibility. */
    abstract val visibility: Property<TerracottaVisibility>

    @get:Input
    /** Release notes for the current version. */
    abstract val changelog: Property<String>

    @get:Input
    /** Canonical project links. */
    abstract val links: Property<TerracottaProjectLinks>

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

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    /** Project icon to upload. */
    abstract val icon: RegularFileProperty

    @get:Input
    /** Gallery images for the project. */
    abstract val gallery: ListProperty<TerracottaGalleryItem>

    /** Applies the computed operations to the remote registry. */
    @TaskAction
    fun apply() =
        runBlocking {
            val local = createLocalProject()

            val providerFactory = findProviderFactory(provider.get())
            val providerLogic = providerFactory.createProviderLogic()
            val stateProvider: StateProvider = providerFactory.createStateProvider(token.orNull)
            val remote = stateProvider.fetchProject(projectId.get())

            val operations = DiffEngine.diff(local, remote, providerLogic.supportsLicenseUrl)
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
            categories = categories.get(),
            license = license.get(),
            licenseUrl = licenseUrl.orNull,
            icon = icon.orNull?.asFile?.absolutePath,
            gallery = gallery.get(),
            links = links.get(),
            visibility = visibility.get(),
        )
    }

    private fun findProviderFactory(id: String): ProviderFactory {
        val loader = ServiceLoader.load(ProviderFactory::class.java)
        return loader.find { it.id == id }
            ?: throw GradleException("No provider found with id '$id'. Make sure the provider is added as a dependency.")
    }
}
