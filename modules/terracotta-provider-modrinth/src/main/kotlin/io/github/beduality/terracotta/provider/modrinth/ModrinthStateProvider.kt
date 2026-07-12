package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.model.TerracottaDonationLink
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

/**
 * Reads project and version state from Modrinth.
 *
 * @see [Modrinth provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-modrinth/tutorials/using-modrinth.html)
 */
class ModrinthStateProvider(private val client: ModrinthClient) : StateProvider {
    /**
     * Fetches the Modrinth project identified by [projectId] and converts it to
     * a [TerracottaProject].
     *
     * @param projectId Modrinth project slug or ID.
     * @return the project state, or `null` if it does not exist.
     */
    override suspend fun fetchProject(projectId: String): TerracottaProject? {
        val project = client.getProject(projectId) ?: return null
        val versions =
            client.getVersions(projectId).map {
                TerracottaVersion(
                    version = it.versionNumber,
                    artifactPath = it.files.firstOrNull()?.filename ?: "",
                    gameVersions = it.gameVersions,
                    loaders = it.loaders,
                    releaseType = TerracottaReleaseType.fromId(it.versionType),
                    changelog = it.changelog ?: "",
                )
            }
        return TerracottaProject(
            id = project.id,
            name = project.title,
            summary = project.summary,
            description = project.body,
            versions = versions,
            tags = project.categories,
            license = project.license.id,
            licenseUrl = project.license.url,
            icon = project.iconUrl,
            gallery =
                project.gallery.map {
                    TerracottaGalleryItem(
                        imagePath = it.url,
                        title = it.title,
                        description = it.description,
                        featured = it.featured,
                        ordering = it.ordering,
                    )
                },
            links =
                TerracottaProjectLinks(
                    issues = project.issuesUrl,
                    source = project.sourceUrl,
                    wiki = project.wikiUrl,
                    community = project.discordUrl,
                    donations = project.donationUrls.map { TerracottaDonationLink(it.platform, it.url) },
                ),
        )
    }
}
