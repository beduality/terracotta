package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

/**
 * Hangar authentication response carrying a short-lived JWT.
 */
@Serializable
data class HangarAuthenticateResponse(
    /** JWT token used for authenticated API requests. */
    val token: String,
    /** Optional expiration timestamp in milliseconds since epoch. */
    val expiresAt: Long? = null,
)
