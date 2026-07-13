package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.ProjectMetadataResolver
import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.io.File

/**
 * Bridges the parsed `terracotta.yml` and Gradle project values into the
 * `terracotta` DSL extension.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 */
internal object TerracottaExtensionConfigurer {
    fun configure(
        extension: TerracottaExtension,
        config: TerracottaConfig,
        project: Project,
    ) {
        val source =
            ProjectMetadataSource(
                name = project.name,
                summary = project.description,
                version = resolveVersion(project),
            )
        val resolved = ProjectMetadataResolver(project.projectDir, config, source).resolve()

        extension.name.convention(resolved.name)
        extension.summary.convention(resolved.summary)
        extension.description.convention(resolved.description)
        extension.categories.primary.id.convention(resolved.categories.primary.id)
        extension.categories.primary.displayName.convention(resolved.categories.primary.displayName)
        resolved.categories.additional.forEach { category ->
            extension.categories.additional.create(category.id) {
                it.id.set(category.id)
                it.displayName.set(category.displayName)
            }
        }
        extension.license.convention(resolved.license)
        resolved.licenseUrl?.let { extension.licenseUrl.convention(it) }
        resolved.icon?.let { extension.icon.convention(project.layout.projectDirectory.file(it)) }
        extension.gameVersions.convention(resolved.gameVersions)
        extension.loaders.convention(resolved.loaders)
        extension.environment.convention(resolved.environment)
        extension.releaseType.convention(resolved.releaseType)
        extension.conventions.readme.convention(resolved.readmeConvention)
        extension.conventions.changelog.convention(resolved.changelogConvention)
        extension.changelog.convention(resolved.changelog)
        resolved.links?.let { links ->
            links.homepage?.let { extension.links.homepage.convention(it) }
            links.source?.let { extension.links.source.convention(it) }
            links.issues?.let { extension.links.issues.convention(it) }
            links.wiki?.let { extension.links.wiki.convention(it) }
            links.community?.let { extension.links.community.convention(it) }
            links.donations.forEach { extension.links.donation(it.platform, it.url) }
            links.other.forEach { (key, value) -> extension.links.other(key, value) }
        }
        resolved.gallery.forEachIndexed { index, item ->
            extension.gallery.create("galleryItem$index") { galleryItem: TerracottaGalleryExtension ->
                galleryItem.imageFile.set(project.file(item.imagePath))
                galleryItem.title.set(item.title)
                galleryItem.description.set(item.description)
                galleryItem.featured.set(item.featured)
                galleryItem.ordering.set(item.ordering)
            }
        }

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
