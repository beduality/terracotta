package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/**
 * Modrinth license information.
 */
@Serializable
data class ModrinthLicense(
    /** SPDX or Modrinth license identifier. */
    val id: String,
    /** Optional URL to the full license text. */
    val url: String? = null,
)
