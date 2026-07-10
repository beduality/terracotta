package io.github.beduality.terracotta.core.model

data class TerracottaVersion(
    val version: String,
    val artifactPath: String,
    val gameVersions: List<String>,
    val loaders: List<TerracottaLoader> = emptyList(),
    val environment: TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY,
)
