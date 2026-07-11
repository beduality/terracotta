package io.github.beduality.terracotta.core.model.version

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType

data class TerracottaVersion(
    val version: String,
    val artifactPath: String,
    val gameVersions: List<String>,
    val loaders: List<String> = emptyList(),
    val environment: TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY,
    val releaseType: TerracottaReleaseType = TerracottaReleaseType.RELEASE,
    val changelog: String = "",
    val displayName: String = "",
)
