package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

@Serializable
data class HangarAuthenticateResponse(
    val token: String,
    val expiresAt: Long? = null,
)
