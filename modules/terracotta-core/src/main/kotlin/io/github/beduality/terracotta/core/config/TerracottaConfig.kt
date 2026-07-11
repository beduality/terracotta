package io.github.beduality.terracotta.core.config

/**
 * In-memory representation of a `terracotta.yml` file.
 *
 * All fields are optional so the file can be partial; missing values fall back
 * to defaults supplied by the caller (for example, the Gradle plugin or a CLI).
 */
data class TerracottaConfig(
    val name: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val license: String? = null,
    val gameVersions: List<String>? = null,
    val loaders: List<String>? = null,
    val environment: String? = null,
    val releaseType: String? = null,
    val changelog: String? = null,
    val convention: TerracottaConventionConfig = TerracottaConventionConfig(),
    val providers: Map<String, TerracottaProviderConfig> = emptyMap(),
)

/**
 * Convention configuration inside `terracotta.yml`.
 */
data class TerracottaConventionConfig(
    val readme: String? = null,
    val changelog: String? = null,
)

/**
 * Provider-specific configuration inside `terracotta.yml`.
 */
data class TerracottaProviderConfig(
    val projectId: String? = null,
    val token: String? = null,
)
