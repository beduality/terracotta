package io.github.beduality.terracotta.provider.modrinth.model

import kotlinx.serialization.Serializable

/**
 * File associated with a Modrinth version.
 */
@Serializable
data class ModrinthVersionFile(
    /** Download URL. */
    val url: String,
    /** File name. */
    val filename: String,
    /** Whether this is the primary file for the version. */
    val primary: Boolean,
)
