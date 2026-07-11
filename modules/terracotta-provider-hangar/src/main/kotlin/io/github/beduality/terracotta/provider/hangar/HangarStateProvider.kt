package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import io.github.beduality.terracotta.provider.hangar.mapper.HangarLoaderMapper
import io.github.beduality.terracotta.provider.hangar.model.HangarVersion

/**
 * Reads project and version state from Hangar.
 *
 * @see [Hangar provider guide](https://beduality.github.io/terracotta/content/sdk/how-to-guides/hangar-provider.html)
 */
class HangarStateProvider(private val client: HangarClient) : StateProvider {
    /**
     * Fetches the Hangar project identified by [projectId] and converts it to a
     * [TerracottaProject].
     *
     * @param projectId Hangar project slug or ID.
     * @return the project state, or `null` if it does not exist.
     */
    override suspend fun fetchProject(projectId: String): TerracottaProject? {
        val project = client.getProject(projectId) ?: return null
        val versions = client.getVersions(projectId).map { toTerracottaVersion(it) }
        return TerracottaProject(
            id = projectId,
            name = project.name,
            summary = project.description,
            description = project.body,
            versions = versions,
            tags = project.tags,
            license = project.license ?: "UNLICENSED",
        )
    }

    private fun toTerracottaVersion(version: HangarVersion): TerracottaVersion {
        return TerracottaVersion(
            version = version.version,
            artifactPath = version.fileName,
            gameVersions = extractGameVersions(version.platformDependencies),
            loaders = extractLoaders(version.platformDependencies),
            environment = TerracottaEnvironment.SERVER_ONLY,
            releaseType = mapReleaseType(version.channel),
        )
    }

    private fun extractGameVersions(platformDependencies: Map<String, List<String>>): List<String> {
        return platformDependencies.values.flatten().distinct()
    }

    private fun extractLoaders(platformDependencies: Map<String, List<String>>): List<String> {
        return platformDependencies.keys.mapNotNull { HangarLoaderMapper.mapPlatformToLoader(it) }
    }

    private fun mapReleaseType(channel: String): TerracottaReleaseType {
        return when (channel.lowercase()) {
            "release" -> TerracottaReleaseType.RELEASE
            "snapshot", "beta" -> TerracottaReleaseType.BETA
            "alpha" -> TerracottaReleaseType.ALPHA
            else -> TerracottaReleaseType.ALPHA
        }
    }
}
