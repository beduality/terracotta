package io.github.beduality.terracotta.provider.hangar.model

import kotlinx.serialization.Serializable

/**
 * Hangar version response.
 */
@Serializable
data class HangarVersion(
    /** Hangar version identifier. */
    val id: String = "",
    /** Version number string. */
    val version: String,
    /** Channel name (e.g. Release, Snapshot). */
    val channel: String,
    /** Release notes. */
    val description: String? = null,
    /** Platform to game-version mapping (e.g. PAPER -> ["1.20.1"]). */
    val platformDependencies: Map<String, List<String>> = emptyMap(),
    /** Primary file name. */
    val fileName: String = "",
)
