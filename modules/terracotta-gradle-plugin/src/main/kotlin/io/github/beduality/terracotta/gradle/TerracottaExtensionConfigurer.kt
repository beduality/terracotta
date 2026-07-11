package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.ProjectMetadataResolver
import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.io.File

internal object TerracottaExtensionConfigurer {
    fun configure(
        extension: TerracottaExtension,
        config: TerracottaConfig,
        project: Project,
    ) {
        val source =
            ProjectMetadataSource(
                name = project.name,
                summary = project.description?.toString(),
                version = resolveVersion(project),
            )
        val resolved = ProjectMetadataResolver(project.projectDir, config, source).resolve()

        extension.name.convention(resolved.name)
        extension.summary.convention(resolved.summary)
        extension.description.convention(resolved.description)
        extension.tags.convention(resolved.tags)
        extension.license.convention(resolved.license)
        extension.gameVersions.convention(resolved.gameVersions)
        extension.loaders.convention(resolved.loaders)
        extension.environment.convention(resolved.environment)
        extension.releaseType.convention(resolved.releaseType)
        extension.conventions.readme.convention(resolved.readmeConvention)
        extension.conventions.changelog.convention(resolved.changelogConvention)
        extension.changelog.convention(resolved.changelog)

        project.plugins.withType(JavaPlugin::class.java) {
            extension.artifactFile.convention(
                project.tasks.named("jar").flatMap { task ->
                    project.layout.file(project.provider { task.outputs.files.singleFile })
                },
            )
        }
    }

    private fun resolveVersion(project: Project): String {
        val version = project.version.toString()
        if (version != "unspecified") return version
        return readGradlePropertiesVersion(project.projectDir) ?: version
    }

    private fun readGradlePropertiesVersion(projectDir: File): String? {
        val file =
            File(projectDir, "gradle.properties").takeIf { it.exists() && it.isFile }
                ?: return null
        val match = Regex("""^version\s*=\s*([^\s]+)""", RegexOption.MULTILINE).find(file.readText())
        return match?.groupValues?.get(1)?.trim()?.takeIf { it != "unspecified" }
    }
}
