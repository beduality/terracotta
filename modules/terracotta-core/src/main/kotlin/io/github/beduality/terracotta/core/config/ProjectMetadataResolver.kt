package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataLoader
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
 *
 * @see [Resolve project metadata guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/resolve-project-metadata.html)
 * @see [Metadata resolution reference](https://beduality.github.io/terracotta/content/modules/core/reference/metadata-resolution.html)
 * @see [Metadata resolution explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/metadata-resolution.html)
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
        /** Version string. */
        val version = resolveVersion(source.version)

        /** Release notes. */
        val changelog = config.changelog ?: (version?.let { detectChangelog(it) } ?: "")

        return ResolvedProjectMetadata(
            name = config.name ?: detected.name ?: "",
            summary = config.summary ?: detected.summary ?: "",
            description = config.description ?: detected.description ?: "",
            categories =
                config.categories
                    ?: TerracottaProjectCategories(
                        primary = TerracottaCategory("uncategorized", "Uncategorized"),
                    ),
            license = config.license ?: detected.license ?: "",
            licenseUrl = config.licenseUrl ?: detected.licenseUrl,
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
            icon = config.icon,
            gallery = config.gallery ?: emptyList(),
            links = config.links ?: detected.links,
            readmeConvention = config.convention.readme ?: "terracotta",
            changelogConvention = config.convention.changelog ?: "keep-a-changelog",
        )
    }

    /**
     * Extracts the changelog section for the given [version] from the project
     * changelog file using the configured changelog convention.
     *
     * @param version the version to look up in the changelog.
     * @return the release notes for [version], or `null` if not found.
     */
    fun detectChangelog(version: String): String? {
        return ChangelogFile.load(cache, changelogConvention).extractVersionSection(version)
    }

    private fun resolveVersion(version: String?): String? {
        return version?.takeIf { it != "unspecified" }
    }
}
