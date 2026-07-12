package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaDonationLink
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Nested `links { ... }` DSL block inside the top-level `terracotta` extension.
 *
 * Values set here become the canonical project links used by every provider.
 * Provider-specific mappings translate these fields into native API calls.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 */
abstract class TerracottaLinksExtension {
    /** Project homepage or website. */
    abstract val homepage: Property<String>

    /** Source code repository URL. */
    abstract val source: Property<String>

    /** Issue tracker URL. */
    abstract val issues: Property<String>

    /** Wiki or documentation URL. */
    abstract val wiki: Property<String>

    /** Community chat or forum URL (e.g., a Discord invite). */
    abstract val community: Property<String>

    /** Donation platform links. */
    abstract val donations: ListProperty<TerracottaDonationLink>

    /** Additional provider-agnostic links keyed by a short identifier. */
    abstract val other: MapProperty<String, String>

    /** Registers a donation link. */
    fun donation(
        platform: String,
        url: String,
    ) {
        donations.add(TerracottaDonationLink(platform, url))
    }

    /** Registers an additional link. */
    fun other(
        id: String,
        url: String,
    ) {
        other.put(id, url)
    }

    /** Builds the canonical project links model from the configured DSL values. */
    fun toModel(): TerracottaProjectLinks =
        TerracottaProjectLinks(
            homepage = homepage.orNull?.takeIf { it.isNotBlank() },
            source = source.orNull?.takeIf { it.isNotBlank() },
            issues = issues.orNull?.takeIf { it.isNotBlank() },
            wiki = wiki.orNull?.takeIf { it.isNotBlank() },
            community = community.orNull?.takeIf { it.isNotBlank() },
            donations = donations.get(),
            other = other.get(),
        )
}
