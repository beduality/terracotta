package io.github.beduality.terracotta.core.model

/**
 * Canonical, provider-agnostic representation of project-related URLs.
 *
 * Providers map these fields to their native link models. Unsupported fields
 * are dropped with a log message when a provider cannot represent them.
 *
 * @property homepage project homepage or website.
 * @property source source code repository URL.
 * @property issues issue tracker URL.
 * @property wiki wiki or documentation URL.
 * @property community community chat or forum URL (e.g., a Discord invite).
 * @property donations donation platform links.
 * @property other additional provider-agnostic links keyed by a short identifier.
 *
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
data class TerracottaProjectLinks(
    /** Homepage or project website. */
    val homepage: String? = null,
    /** Source code repository. */
    val source: String? = null,
    /** Issue tracker. */
    val issues: String? = null,
    /** Wiki or documentation. */
    val wiki: String? = null,
    /** Community chat / forum (e.g., Discord invite). */
    val community: String? = null,
    /** Donation links. */
    val donations: List<TerracottaDonationLink> = emptyList(),
    /** Additional provider-agnostic links keyed by a short identifier. */
    val other: Map<String, String> = emptyMap(),
) {
    /** Returns `true` if no link fields are populated. */
    fun isEmpty(): Boolean =
        homepage == null &&
            source == null &&
            issues == null &&
            wiki == null &&
            community == null &&
            donations.isEmpty() &&
            other.isEmpty()
}
