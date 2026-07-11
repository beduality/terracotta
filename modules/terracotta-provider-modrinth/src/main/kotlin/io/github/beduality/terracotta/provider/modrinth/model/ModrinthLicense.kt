package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/**
 * Modrinth license identifier.
 */
@Serializable
data class ModrinthLicense(
    /** SPDX or Modrinth license identifier. */
    val id: String,
)
