package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.detect.ProjectMetadataLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ChangelogFile
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.ProjectFileConventionRegistry
import io.github.beduality.terracotta.core.model.projectfile.convention.ReadmeConvention
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import java.io.File

/**
 * Resolves the effective project metadata for a project directory by merging
 * values from `terracotta.yml`, auto-detection, and caller-supplied defaults.
 *
 * The resolution is build-tool agnostic; callers such as the Gradle plugin pass
 * in the project directory, the parsed [TerracottaConfig], and a
 * [ProjectMetadataSource] for values that come from the build system.
 */
class ProjectMetadataResolver(
    private val projectDir: File,
    private val config: TerracottaConfig,
    private val source: ProjectMetadataSource,
) {
    private val cache = ProjectFileCache(projectDir)
    private val readmeConvention: ReadmeConvention
    private val changelogConvention: ChangelogConvention
    private val detected: io.github.beduality.terracotta.core.model.metadata.ProjectMetadata

    init {
        ProjectFileConventionRegistry.load()
        readmeConvention = ProjectFileConventionRegistry.resolve(config.convention.readme ?: "terracotta") as ReadmeConvention
        changelogConvention =
            ProjectFileConventionRegistry.resolve(
                config.convention.changelog ?: "keep-a-changelog",
            ) as ChangelogConvention
        detected = ProjectMetadataLoader.load(ProjectMetadataContext(cache, source, readmeConvention, changelogConvention))
    }

    /**
     * Returns the fully resolved project metadata.
     *
     * The changelog is resolved from the configured version. For lazy changelog
     * resolution (e.g. when the build version may change later), use
     * [detectChangelog] instead.
     */
    fun resolve(): ResolvedProjectMetadata {
        val version = resolveVersion(source.version)
        val changelog = config.changelog ?: (version?.let { detectChangelog(it) } ?: "")

        return ResolvedProjectMetadata(
            name = config.name ?: detected.name ?: "",
            summary = config.summary ?: detected.summary ?: "",
            description = config.description ?: detected.description ?: "",
            tags = config.tags ?: emptyList(),
            license = config.license ?: detected.license ?: "",
            gameVersions = config.gameVersions ?: detected.gameVersions ?: emptyList(),
            loaders = config.loaders ?: detected.loaders ?: emptyList(),
            environment =
                config.environment?.let { TerracottaEnvironment.fromId(it) }
                    ?: detected.environment
                    ?: TerracottaEnvironment.SERVER_ONLY,
            releaseType =
                config.releaseType?.let { TerracottaReleaseType.fromId(it) }
                    ?: detected.releaseType
                    ?: TerracottaReleaseType.RELEASE,
            changelog = changelog,
            readmeConvention = config.convention.readme ?: "terracotta",
            changelogConvention = config.convention.changelog ?: "keep-a-changelog",
        )
    }

    /**
     * Extracts the changelog section for the given [version] from the project
     * changelog file using the configured changelog convention.
     */
    fun detectChangelog(version: String): String? {
        return ChangelogFile.load(cache, changelogConvention).extractVersionSection(version)
    }

    private fun resolveVersion(version: String?): String? {
        if (version != null && version != "unspecified") return version
        return readGradlePropertiesVersion(projectDir)
    }

    private fun readGradlePropertiesVersion(projectDir: File): String? {
        val file =
            File(projectDir, "gradle.properties").takeIf { it.exists() && it.isFile }
                ?: return null
        val match = Regex("""^version\s*=\s*([^\s]+)""", RegexOption.MULTILINE).find(file.readText())
        return match?.groupValues?.get(1)?.trim()
    }
}
