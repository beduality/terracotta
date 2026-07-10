package io.github.beduality.terracotta.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class TerracottaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("terracotta", TerracottaExtension::class.java)

        extension.tags.convention(emptyList())
        extension.loaders.convention(emptyList())
        extension.environment.convention("server_only")
        extension.provider.convention("modrinth")
        extension.token.convention(System.getenv("TERRACOTTA_TOKEN"))

        project.plugins.withType(JavaPlugin::class.java) {
            extension.artifactFile.convention(project.tasks.named("jar").flatMap { it.outputs.files.singleFile })
        }

        val planTask = project.tasks.register("terracottaPlan", TerracottaPlanTask::class.java) {
            it.description = "Plans changes to apply to the registry"
            it.group = "terracotta"

            it.projectId.set(extension.projectId)
            it.name.set(extension.name)
            it.summary.set(extension.summary)
            it.description.set(extension.description)
            it.tags.set(extension.tags)
            it.license.set(extension.license)
            it.gameVersions.set(extension.gameVersions)
            it.loaders.set(extension.loaders)
            it.environment.set(extension.environment)
            it.provider.set(extension.provider)
            it.token.set(extension.token)
            it.artifactFile.set(extension.artifactFile)
        }

        project.tasks.register("terracottaApply", TerracottaApplyTask::class.java) {
            it.description = "Applies changes to the registry"
            it.group = "terracotta"

            it.projectId.set(extension.projectId)
            it.name.set(extension.name)
            it.summary.set(extension.summary)
            it.description.set(extension.description)
            it.tags.set(extension.tags)
            it.license.set(extension.license)
            it.gameVersions.set(extension.gameVersions)
            it.loaders.set(extension.loaders)
            it.environment.set(extension.environment)
            it.provider.set(extension.provider)
            it.token.set(extension.token)
            it.artifactFile.set(extension.artifactFile)

            it.dependsOn(planTask)
        }
    }
}
