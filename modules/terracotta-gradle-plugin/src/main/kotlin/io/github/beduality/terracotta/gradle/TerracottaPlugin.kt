package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaReleaseType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class TerracottaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("terracotta", TerracottaExtension::class.java)

        extension.tags.convention(emptyList())
        extension.loaders.convention(emptyList())
        extension.environment.convention(TerracottaEnvironment.SERVER_ONLY)
        extension.releaseType.convention(TerracottaReleaseType.RELEASE)
        extension.changelog.convention("")

        project.plugins.withType(JavaPlugin::class.java) {
            extension.artifactFile.convention(
                project.tasks.named("jar").flatMap { task ->
                    project.layout.file(project.provider { task.outputs.files.singleFile })
                }
            )
        }

        val allPlanTasks = mutableListOf<Any>()
        val allApplyTasks = mutableListOf<Any>()

        extension.providers.all { providerExt ->
            val providerId = providerExt.name
            // Set convention: <PROVIDER_NAME>_TOKEN environment variable (uppercase)
            providerExt.token.convention(System.getenv("${providerId.uppercase()}_TOKEN"))

            val providerPlanTask = project.tasks.register("terracottaPlan${providerId.replaceFirstChar(Char::titlecase)}", TerracottaPlanTask::class.java) { task ->
                task.setDescription("Plans changes to apply to $providerId")
                task.group = "terracotta"

                task.projectId.set(providerExt.projectId)
                task.modName.set(extension.name)
                task.summary.set(extension.summary)
                task.modDescription.set(extension.description)
                task.tags.set(extension.tags)
                task.license.set(extension.license)
                task.gameVersions.set(extension.gameVersions)
                task.loaders.set(extension.loaders)
                task.environment.set(extension.environment)
                task.releaseType.set(extension.releaseType)
                task.changelog.set(extension.changelog)
                task.provider.set(providerId)
                task.token.set(providerExt.token)
                task.artifactFile.set(extension.artifactFile)
            }
            allPlanTasks.add(providerPlanTask)

            val providerApplyTask = project.tasks.register("terracottaApply${providerId.replaceFirstChar(Char::titlecase)}", TerracottaApplyTask::class.java) { task ->
                task.setDescription("Applies changes to $providerId")
                task.group = "terracotta"

                task.projectId.set(providerExt.projectId)
                task.modName.set(extension.name)
                task.summary.set(extension.summary)
                task.modDescription.set(extension.description)
                task.tags.set(extension.tags)
                task.license.set(extension.license)
                task.gameVersions.set(extension.gameVersions)
                task.loaders.set(extension.loaders)
                task.environment.set(extension.environment)
                task.releaseType.set(extension.releaseType)
                task.changelog.set(extension.changelog)
                task.provider.set(providerId)
                task.token.set(providerExt.token)
                task.artifactFile.set(extension.artifactFile)

                task.dependsOn(providerPlanTask)
            }
            allApplyTasks.add(providerApplyTask)
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
    }
}
