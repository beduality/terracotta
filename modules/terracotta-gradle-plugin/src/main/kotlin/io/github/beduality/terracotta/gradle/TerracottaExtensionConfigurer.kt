package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.detect.ProjectMetadataLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ChangelogFile
import io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.ProjectFileConventionRegistry
import io.github.beduality.terracotta.core.model.projectfile.convention.ReadmeConvention
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.io.File

internal object TerracottaExtensionConfigurer {
    fun configure(
        extension: TerracottaExtension,
        config: TerracottaConfig,
        project: Project,
    ) {
        ProjectFileConventionRegistry.load()

        val cache = ProjectFileCache(project.projectDir)
        val source =
            ProjectMetadataSource(
                name = project.name,
                summary = project.description?.toString(),
                version = project.version.toString(),
            )
        val readmeConvention = ProjectFileConventionRegistry.resolve(config.convention.readme ?: "terracotta") as ReadmeConvention
        val changelogConvention = ProjectFileConventionRegistry.resolve(config.convention.changelog ?: "keep-a-changelog") as ChangelogConvention
        val context = ProjectMetadataContext(cache, source, readmeConvention, changelogConvention)
        val detected = ProjectMetadataLoader.load(context)

        extension.name.convention(config.name ?: detected.name ?: project.name)
        extension.summary.convention(config.summary ?: detected.summary ?: "")
        extension.description.convention(config.description ?: detected.description ?: "")
        extension.tags.convention(config.tags ?: emptyList())
        extension.license.convention(config.license ?: detected.license ?: "")
        extension.gameVersions.convention(config.gameVersions ?: detected.gameVersions ?: emptyList())
        extension.loaders.convention(
            config.loaders ?: detected.loaders ?: emptyList(),
        )
        extension.environment.convention(
            config.environment?.let { environmentId -> TerracottaEnvironment.fromId(environmentId) }
                ?: detected.environment
                ?: TerracottaEnvironment.SERVER_ONLY,
        )
        extension.releaseType.convention(
            config.releaseType?.let { releaseTypeId -> TerracottaReleaseType.fromId(releaseTypeId) }
                ?: detected.releaseType
                ?: TerracottaReleaseType.RELEASE,
        )
        extension.conventions.readme.convention(config.convention.readme ?: "terracotta")
        extension.conventions.changelog.convention(config.convention.changelog ?: "keep-a-changelog")
        extension.changelog.convention(
            project.provider {
                config.changelog ?: detectChangelog(project, changelogConvention) ?: ""
            },
        )

        project.plugins.withType(JavaPlugin::class.java) {
            extension.artifactFile.convention(
                project.tasks.named("jar").flatMap { task ->
                    project.layout.file(project.provider { task.outputs.files.singleFile })
                },
            )
        }
    }

    private fun detectChangelog(
        project: Project,
        convention: io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention,
    ): String? {
        val version = resolveProjectVersion(project) ?: return null
        return ChangelogFile.load(ProjectFileCache(project.projectDir), convention)
            .extractVersionSection(version)
    }

    private fun resolveProjectVersion(project: Project): String? {
        val projectVersion = project.version.toString()
        if (projectVersion != "unspecified") {
            return projectVersion
        }
        return readGradlePropertiesVersion(project.projectDir)
    }

    private fun readGradlePropertiesVersion(projectDir: File): String? {
        val file = File(projectDir, "gradle.properties").takeIf { it.exists() && it.isFile }
            ?: return null
        val match = Regex("""^version\s*=\s*([^\s]+)""", RegexOption.MULTILINE).find(file.readText())
        return match?.groupValues?.get(1)?.trim()
    }
}
