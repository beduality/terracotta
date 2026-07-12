package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/** A donation link returned by the Modrinth API. */
@Serializable
data class ModrinthDonationUrl(
    /** Donation platform identifier. */
    val id: String,
    /** Human-readable donation platform name. */
    val platform: String,
    /** URL to the donation page. */
    val url: String,
)
