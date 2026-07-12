package io.github.beduality.terracotta.core.state

import java.time.Instant

/**
 * Root persisted state for a Terracotta project.
 *
 * @property version Schema version of the persisted file. Incremented when the
 *   on-disk format changes in a non-backwards-compatible way.
 * @property lastRun Summary of the most recent run that touched state.
 * @property projectId Stable project identifier used by Terracotta.
 * @property providers Per-provider state records.
 */
data class TerracottaState(
    val version: Int = 1,
    val lastRun: RunSummary? = null,
    val projectId: String? = null,
    val providers: Map<String, ProviderState> = emptyMap(),
)

/**
 * @property command The command/task that ran (e.g. `apply`, `plan`).
 * @property startedAt When the run began.
 * @property finishedAt When the run finished, if known.
 * @property commitSha Optional VCS commit SHA captured from the environment.
 */
data class RunSummary(
    val command: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val commitSha: String? = null,
)

/**
 * @property versionIds IDs of versions known to have been published.
 * @property gallery Gallery item identities keyed by stable local identifier.
 * @property metadataHash Hash of the resolved metadata that produced this state.
 */
data class ProviderState(
    val versionIds: List<String> = emptyList(),
    val gallery: Map<String, GalleryItemIdentity> = emptyMap(),
    val metadataHash: String? = null,
)

/**
 * Stable identity for a single gallery item, used for cross-run matching.
 *
 * @property localKey Stable key from the local configuration.
 * @property remoteUrl Remote URL assigned by the provider after upload.
 * @property remoteId Optional provider-specific identifier.
 */
data class GalleryItemIdentity(
    val localKey: String,
    val remoteUrl: String? = null,
    val remoteId: String? = null,
)
