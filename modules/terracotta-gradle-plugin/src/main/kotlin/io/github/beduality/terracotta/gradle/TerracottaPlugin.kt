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
            extension.artifactFile.convention(project.tasks.named("jar").flatMap { it.outputs.files.singleFile })
        }

        val allPlanTasks = mutableListOf<Any>()
        val allApplyTasks = mutableListOf<Any>()

        extension.providers.all { providerExt ->
            val providerId = providerExt.name
            // Set convention: <PROVIDER_NAME>_TOKEN environment variable (uppercase)
            providerExt.token.convention(System.getenv("${providerId.uppercase()}_TOKEN"))

            val providerPlanTask = project.tasks.register("terracottaPlan${providerId.replaceFirstChar(Char::titlecase)}", TerracottaPlanTask::class.java) {
                it.description = "Plans changes to apply to $providerId"
                it.group = "terracotta"

                it.projectId.set(providerExt.projectId)
                it.name.set(extension.name)
                it.summary.set(extension.summary)
                it.description.set(extension.description)
                it.tags.set(extension.tags)
                it.license.set(extension.license)
                it.gameVersions.set(extension.gameVersions)
                it.loaders.set(extension.loaders)
                it.environment.set(extension.environment)
                it.releaseType.set(extension.releaseType)
                it.changelog.set(extension.changelog)
                it.provider.set(providerId)
                it.token.set(providerExt.token)
                it.artifactFile.set(extension.artifactFile)
            }
            allPlanTasks.add(providerPlanTask)

            val providerApplyTask = project.tasks.register("terracottaApply${providerId.replaceFirstChar(Char::titlecase)}", TerracottaApplyTask::class.java) {
                it.description = "Applies changes to $providerId"
                it.group = "terracotta"

                it.projectId.set(providerExt.projectId)
                it.name.set(extension.name)
                it.summary.set(extension.summary)
                it.description.set(extension.description)
                it.tags.set(extension.tags)
                it.license.set(extension.license)
                it.gameVersions.set(extension.gameVersions)
                it.loaders.set(extension.loaders)
                it.environment.set(extension.environment)
                it.releaseType.set(extension.releaseType)
                it.changelog.set(extension.changelog)
                it.provider.set(providerId)
                it.token.set(providerExt.token)
                it.artifactFile.set(extension.artifactFile)

                it.dependsOn(providerPlanTask)
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
