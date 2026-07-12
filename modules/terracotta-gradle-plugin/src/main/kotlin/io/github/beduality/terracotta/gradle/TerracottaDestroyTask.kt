package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.StateProvider
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.ServiceLoader

/**
 * Gradle task that destroys a Terracotta project on a remote registry.
 *
 * By default the entire remote project is deleted. Use `--versions-only` to delete
 * only the project's versions while keeping the project page.
 *
 * This task requires explicit confirmation. In interactive environments it prompts
 * for `y/N`. In non-interactive environments (e.g. CI) pass `--force`.
 *
 * @see [Tasks reference](https://beduality.github.io/terracotta/content/modules/gradle-plugin/reference/tasks.html)
 */
@DisableCachingByDefault(because = "Task makes destructive network calls to remote registries")
abstract class TerracottaDestroyTask : DefaultTask() {
    @get:Input
    /** Registry project identifier. */
    abstract val projectId: Property<String>

    @get:Input
    /** Provider identifier. */
    abstract val provider: Property<String>

    @get:Input
    @get:Optional
    /** Authentication token for the provider registry. */
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    /** Whether to delete only versions and keep the project page. */
    abstract val versionsOnly: Property<Boolean>

    @get:Input
    @get:Optional
    /** Whether to print what would be destroyed without making remote calls. */
    abstract val dryRun: Property<Boolean>

    @get:Input
    @get:Optional
    /** Whether to skip the confirmation prompt. Required in non-interactive environments. */
    abstract val force: Property<Boolean>

    /** Destroys the remote project or its versions according to the configured options. */
    @TaskAction
    fun destroy() =
        runBlocking {
            val providerFactory = findProviderFactory(provider.get())
            val destructiveProvider =
                providerFactory.createDestructiveRegistryProvider(token.orNull)
                    ?: throw GradleException(
                        "Provider '${provider.get()}' does not support destructive operations.",
                    )

            val isDryRun = dryRun.orNull == true
            val isVersionsOnly = versionsOnly.orNull == true

            if (isDryRun) {
                reportPlannedDestruction(providerFactory)
                return@runBlocking
            }

            confirmOrFail()

            logger.lifecycle("Terracotta Destroy:")
            val target = projectId.get()
            if (isVersionsOnly) {
                logger.lifecycle("  Deleting all versions of project '$target' on ${provider.get()}...")
                destructiveProvider.deleteAllVersions(target)
                logger.lifecycle("  Done!")
            } else {
                logger.lifecycle("  Deleting project '$target' on ${provider.get()}...")
                destructiveProvider.deleteProject(target)
                logger.lifecycle("  Done!")
            }
        }

    private suspend fun reportPlannedDestruction(providerFactory: ProviderFactory) {
        val stateProvider: StateProvider = providerFactory.createStateProvider(token.orNull)
        val remote = stateProvider.fetchProject(projectId.get())

        logger.lifecycle("Terracotta Destroy (dry run):")
        if (remote == null) {
            logger.lifecycle("  Project '${projectId.get()}' does not exist on ${provider.get()}; nothing to destroy.")
            return
        }

        if (versionsOnly.orNull == true) {
            if (remote.versions.isEmpty()) {
                logger.lifecycle("  Project '${projectId.get()}' has no versions to delete on ${provider.get()}.")
            } else {
                logger.lifecycle("  Would delete ${remote.versions.size} version(s) of project '${projectId.get()}' on ${provider.get()}:")
                remote.versions.forEach { version ->
                    logger.lifecycle("    - ${version.version}")
                }
            }
        } else {
            logger.lifecycle("  Would delete project '${projectId.get()}' on ${provider.get()}.")
            if (remote.versions.isNotEmpty()) {
                logger.lifecycle("  This would also remove ${remote.versions.size} version(s).")
            }
        }
    }

    private fun confirmOrFail() {
        if (force.orNull == true) {
            return
        }

        val console = System.console()
        if (console == null) {
            throw GradleException(
                "This is a destructive operation. Run with --force to confirm.",
            )
        }

        val target = projectId.get()
        val operation = if (versionsOnly.orNull == true) "delete all versions of" else "delete"
        val prompt = "This will $operation project '$target' on ${provider.get()}. Are you sure? [y/N] "
        val response = console.readLine(prompt)
        if (response == null || !response.trim().equals("y", ignoreCase = true)) {
            throw GradleException("Destroy cancelled by user.")
        }
    }

    private fun findProviderFactory(id: String): ProviderFactory {
        val loader = ServiceLoader.load(ProviderFactory::class.java)
        return loader.find { it.id == id }
            ?: throw GradleException("No provider found with id '$id'. Make sure the provider is added as a dependency.")
    }
}
