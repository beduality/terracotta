package io.github.beduality.terracotta.core.model

/**
 * A link to a donation platform for the project.
 *
 * @property platform short identifier for the donation platform, such as
 *     `patreon`, `bmac`, or `ko-fi`.
 * @property url URL to the project's donation page on that platform.
 *
 * @see [TerracottaProjectLinks]
 * @see [Models reference](https://beduality.github.io/terracotta/content/modules/core/reference/models.html)
 */
data class TerracottaDonationLink(
    /** Donation platform identifier (e.g., `patreon`, `bmac`, `ko-fi`). */
    val platform: String,
    /** URL to the donation page. */
    val url: String,
)
