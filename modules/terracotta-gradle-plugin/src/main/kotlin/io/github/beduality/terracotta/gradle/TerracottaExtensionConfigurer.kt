package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaReleaseType
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

internal object TerracottaExtensionConfigurer {
    fun configure(
        extension: TerracottaExtension,
        config: TerracottaConfig,
        project: Project,
    ) {
        config.name?.let { extension.name.convention(it) }
        config.summary?.let { extension.summary.convention(it) }
        config.description?.let { extension.description.convention(it) }
        extension.tags.convention(config.tags ?: emptyList())
        config.license?.let { extension.license.convention(it) }
        extension.gameVersions.convention(config.gameVersions ?: emptyList())
        extension.loaders.convention(
            config.loaders?.map { loaderId -> TerracottaLoader.fromId(loaderId) } ?: emptyList(),
        )
        extension.environment.convention(
            config.environment?.let { environmentId -> TerracottaEnvironment.fromId(environmentId) }
                ?: TerracottaEnvironment.SERVER_ONLY,
        )
        extension.releaseType.convention(
            config.releaseType?.let { releaseTypeId -> TerracottaReleaseType.fromId(releaseTypeId) }
                ?: TerracottaReleaseType.RELEASE,
        )
        extension.changelog.convention(config.changelog ?: "")

        project.plugins.withType(JavaPlugin::class.java) {
            extension.artifactFile.convention(
                project.tasks.named("jar").flatMap { task ->
                    project.layout.file(project.provider { task.outputs.files.singleFile })
                },
            )
        }
    }
}
