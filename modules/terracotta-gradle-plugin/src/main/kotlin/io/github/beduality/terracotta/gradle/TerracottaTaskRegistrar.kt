package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import org.gradle.api.Project

/**
 * Registers the `terracottaPlan` and `terracottaApply` tasks for each configured
 * provider.
 *
 * @see [Tasks reference](https://beduality.github.io/terracotta/content/modules/gradle-plugin/reference/tasks.html)
 */
internal object TerracottaTaskRegistrar {
    fun registerTasks(
        extension: TerracottaExtension,
        config: TerracottaConfig,
        project: Project,
    ) {
        val allPlanTasks = mutableListOf<Any>()
        val allApplyTasks = mutableListOf<Any>()
        val allDestroyTasks = mutableListOf<Any>()

        extension.providers.all { providerExt ->
            val providerId = providerExt.name
            val fileProvider = config.providers[providerId]

            // Token convention: explicit YAML value wins over the <PROVIDER>_TOKEN env var.
            val envToken = System.getenv("${providerId.uppercase()}_TOKEN")
            providerExt.token.convention(fileProvider?.token ?: envToken)
            fileProvider?.projectId?.let { providerExt.projectId.convention(it) }

            val providerPlanTask =
                project.tasks.register(
                    "terracottaPlan${providerId.replaceFirstChar(Char::titlecase)}",
                    TerracottaPlanTask::class.java,
                ) { task ->
                    task.setDescription("Plans changes to apply to $providerId")
                    task.group = "terracotta"

                    task.projectId.set(providerExt.projectId)
                    task.modName.set(extension.name)
                    task.summary.set(extension.summary)
                    task.modDescription.set(extension.description)
                    task.tags.set(extension.tags)
                    task.license.set(extension.license)
                    task.licenseUrl.set(extension.licenseUrl)
                    task.gameVersions.set(extension.gameVersions)
                    task.loaders.set(extension.loaders)
                    task.environment.set(extension.environment)
                    task.releaseType.set(extension.releaseType)
                    task.changelog.set(extension.changelog)
                    task.provider.set(providerId)
                    task.token.set(providerExt.token)
                    task.artifactFile.set(extension.artifactFile)
                    task.icon.set(extension.icon)
                    task.gallery.convention(galleryItemsProvider(project, extension))
                }
            allPlanTasks.add(providerPlanTask)

            val providerApplyTask =
                project.tasks.register(
                    "terracottaApply${providerId.replaceFirstChar(Char::titlecase)}",
                    TerracottaApplyTask::class.java,
                ) { task ->
                    task.setDescription("Applies changes to $providerId")
                    task.group = "terracotta"

                    task.projectId.set(providerExt.projectId)
                    task.modName.set(extension.name)
                    task.summary.set(extension.summary)
                    task.modDescription.set(extension.description)
                    task.tags.set(extension.tags)
                    task.license.set(extension.license)
                    task.licenseUrl.set(extension.licenseUrl)
                    task.gameVersions.set(extension.gameVersions)
                    task.loaders.set(extension.loaders)
                    task.environment.set(extension.environment)
                    task.releaseType.set(extension.releaseType)
                    task.changelog.set(extension.changelog)
                    task.provider.set(providerId)
                    task.token.set(providerExt.token)
                    task.artifactFile.set(extension.artifactFile)
                    task.icon.set(extension.icon)
                    task.gallery.convention(galleryItemsProvider(project, extension))

                    task.dependsOn(providerPlanTask)
                }
            allApplyTasks.add(providerApplyTask)

            val providerDestroyTask =
                project.tasks.register(
                    "terracottaDestroy${providerId.replaceFirstChar(Char::titlecase)}",
                    TerracottaDestroyTask::class.java,
                ) { task ->
                    task.setDescription("Destroys the project on $providerId")
                    task.group = "terracotta"

                    task.projectId.set(providerExt.projectId)
                    task.provider.set(providerId)
                    task.token.set(providerExt.token)
                }
            allDestroyTasks.add(providerDestroyTask)
        }

        // Create providers declared only in terracotta.yml after the build script has run,
        // so the Kotlin DSL can still create or configure providers without name collisions.
        project.afterEvaluate {
            config.providers.keys.forEach { providerId ->
                if (extension.providers.findByName(providerId) == null) {
                    extension.providers.create(providerId)
                }
            }
        }

        project.tasks.register("terracottaPlan") {
            it.description = "Plans changes to apply to all configured providers"
            it.group = "terracotta"
            it.dependsOn(allPlanTasks)
        }

        project.tasks.register("terracottaApply") {
            it.description = "Applies changes to all configured providers"
            it.group = "terracotta"
            it.dependsOn(allApplyTasks)
        }

        project.tasks.register("terracottaDestroy") {
            it.description = "Destroys the project on all configured providers"
            it.group = "terracotta"
            it.dependsOn(allDestroyTasks)
        }
    }

    private fun galleryItemsProvider(
        project: Project,
        extension: TerracottaExtension,
    ) = project.provider {
        extension.gallery.map { item ->
            TerracottaGalleryItem(
                imagePath = item.imageFile.get().asFile.absolutePath,
                title = item.title.orNull ?: "",
                description = item.description.orNull ?: "",
                featured = item.featured.orNull ?: false,
                ordering = item.ordering.orNull ?: 0,
            )
        }
    }
}
