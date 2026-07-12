package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

/** A donation link returned by the Hangar API. */
@Serializable
data class HangarDonationLink(
    /** Donation platform identifier. */
    val platform: String,
    /** URL to the donation page. */
    val url: String,
)
