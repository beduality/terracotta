package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaGalleryItem

/**
 * In-memory representation of a `terracotta.yml` file.
 *
 * All fields are optional so the file can be partial; missing values fall back
 * to defaults supplied by the caller (for example, the Gradle plugin or a CLI).
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 * @see [Load a terracotta.yml file](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/load-terracotta-config.html)
 */
data class TerracottaConfig(
    /** Project display name. */
    val name: String? = null,
    /** Short project summary or tagline. */
    val summary: String? = null,
    /** Full project description. */
    val description: String? = null,
    /** Search tags. */
    val tags: List<String>? = null,
    /** SPDX license identifier. */
    val license: String? = null,
    /** Optional URL to the full license text. */
    val licenseUrl: String? = null,
    /** Supported Minecraft game versions. */
    val gameVersions: List<String>? = null,
    /** Supported loader identifiers. */
    val loaders: List<String>? = null,
    /** Runtime environment identifier (`client_only`, `server_only`, or `universal`). */
    val environment: String? = null,
    /** Release type identifier (`release`, `beta`, or `alpha`). */
    val releaseType: String? = null,
    /** Changelog text for the current version. */
    val changelog: String? = null,
    /** Gallery images for the project. */
    val gallery: List<TerracottaGalleryItem>? = null,
    /** README and changelog convention identifiers. */
    val convention: TerracottaConventionConfig = TerracottaConventionConfig(),
    /** Per-provider configuration keyed by provider ID. */
    val providers: Map<String, TerracottaProviderConfig> = emptyMap(),
)
