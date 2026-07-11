# Proposal: Terracotta Cloud

**Date**: 2026-07-11  
**Status**: Draft  
**Related**: Terracotta Registry Gateway, External YAML Configuration, Authentication Workflows

## Summary

Introduce **Terracotta Cloud**, a hosted service that provides centralized **lock management** and **state synchronization** for Terracotta projects. It eliminates race conditions during CI/CD releases, gives teams a shared source of truth for published versions, and enables future hosted workflows such as the Registry Gateway.

## Problem Statement

Today, Terracotta runs entirely on the user's machine or CI runner. Each execution is stateless and independent, which causes several operational problems:

1. **Race conditions**: Multiple CI jobs or team members can attempt to publish the same version to the same registry at the same time.
2. **Lost state**: If a local `terracottaApply` succeeds but the CI cache is cleared, the next run cannot tell what has already been published.
3. **Duplicate retries**: Failed uploads that partially succeed may be retried, creating duplicate versions or inconsistent metadata.
4. **No team visibility**: There is no shared ledger of who published what, when, and from which commit.
5. **Gateway blocker**: A future hosted distribution gateway needs a reliable place to store account links, project mappings, and publish history.

## Goals

- Prevent concurrent publishes of the same version through distributed locks.
- Store canonical publish state in the cloud.
- Synchronize state across local machines and CI runners.
- Keep the local tool fully functional when the cloud is unreachable (graceful degradation).
- Lay the infrastructure foundation for the Terracotta Registry Gateway.

## Non-Goals

- Replace existing registry providers (Modrinth, CurseForge, Hangar, etc.).
- Store source code or build artifacts long-term.
- Mandate cloud usage; it must remain optional.

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Terracotta Cloud                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Lock Service │  │ State Store  │  │   Project Registry   │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└────────────────────┬───────────────────────────────────────────┘
                     │ HTTPS / WebSocket
┌────────────────────┴───────────────────────────────────────────┐
│                     Terracotta Clients                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Local Gradle │  │ CI Runner    │  │ Registry Gateway     │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. Lock Service

Distributed locks guard critical sections during publish operations.

```kotlin
interface LockService {
    suspend fun acquire(
        projectId: String,
        version: String,
        provider: String,
        holder: String,
        ttl: Duration = Duration.ofMinutes(5)
    ): LockResult

    suspend fun release(lockId: String)
    suspend fun extend(lockId: String, ttl: Duration): Boolean
}

sealed class LockResult {
    data class Acquired(val lockId: String) : LockResult()
    data class Held(val holder: String, val expiresAt: Instant) : LockResult()
    data class Error(val message: String) : LockResult()
}
```

Rules:

- A lock key is `projects/{projectId}/versions/{version}/providers/{provider}`.
- Locks have a TTL and must be heartbeated during long uploads.
- If the cloud is unavailable, the client may proceed in **local-only mode** after logging a warning.

### 2. State Store

The state store holds the authoritative record of what has been published.

```kotlin
@Serializable
data class PublishedVersion(
    val projectId: String,
    val version: String,
    val provider: String,
    val externalId: String?,        // Registry-assigned version or file ID
    val externalUrl: String?,       // Link to the published page
    val commitSha: String?,
    val publishedBy: String,
    val publishedAt: Instant,
    val artifacts: List<PublishedArtifact>,
    val metadataHash: String,         // Hash of the resolved metadata sent
)

@Serializable
data class PublishedArtifact(
    val fileName: String,
    val checksum: String,
    val externalFileId: String? = null,
)
```

State operations:

```kotlin
interface StateService {
    suspend fun getPublishedVersions(
        projectId: String,
        provider: String? = null
    ): List<PublishedVersion>

    suspend fun recordPublish(
        version: PublishedVersion
    ): Result<Unit>

    suspend fun getLatestVersion(projectId: String): PublishedVersion?
}
```

### 3. Project Registry

Maps local project identifiers to cloud projects and enforces access control.

```kotlin
@Serializable
data class CloudProject(
    val id: String,
    val ownerId: String,
    val name: String,
    val repositoryUrl: String?,
    val createdAt: Instant,
    val allowedProviders: List<String>,
)

interface ProjectService {
    suspend fun createProject(name: String, repositoryUrl: String?): CloudProject
    suspend fun getProject(id: String): CloudProject?
    suspend fun listProjects(): List<CloudProject>
}
```

## Client Integration

### Gradle Plugin / CLI

```yaml
# terracotta.yaml
cloud:
  enabled: true
  endpoint: https://cloud.terracotta.dev # default
  projectId: ${TERRACOTTA_CLOUD_PROJECT_ID}
  token: ${TERRACOTTA_CLOUD_TOKEN}
  lockTtl: 5m
  syncOnPlan: true
```

Behavior:

1. `terracottaPlan` fetches remote state and includes it in the diff.
2. `terracottaApply` acquires the lock before publishing, releases it after success or failure.
3. After each successful provider publish, the client records the result to the cloud.

```kotlin
class CloudSynchronizer(
    private val stateService: StateService,
    private val lockService: LockService,
) {
    suspend fun <T> withPublishLock(
        projectId: String,
        version: String,
        provider: String,
        block: suspend () -> T
    ): T {
        val result = lockService.acquire(projectId, version, provider, holder = ciRunId())
        when (result) {
            is LockResult.Acquired -> try {
                return block()
            } finally {
                lockService.release(result.lockId)
            }
            is LockResult.Held -> throw PublishLockedException(
                "Version $version is already being published by ${result.holder}"
            )
            is LockResult.Error -> if (cloudMandatory()) {
                throw PublishLockException(result.message)
            } else {
                logger.warn("Cloud lock unavailable, proceeding locally: ${result.message}")
                return block()
            }
        }
    }
}
```

## State Synchronization Strategy

### Two-Way Sync

- **Pull**: Before planning, fetch published versions from the cloud.
- **Push**: After applying, push the new state.
- **Conflict resolution**: If local and remote state diverge, prefer remote for completed publishes and local for in-flight operations.

### Offline Mode

When `cloud.enabled` is true but the service is unreachable:

- Log a clear warning.
- Continue with local state if `failOnCloudError` is false (default).
- Optionally queue state updates and retry later.

## Security

- API tokens are short-lived JWTs scoped to a project or organization.
- All traffic is over HTTPS with certificate pinning recommended for CI.
- State records do not contain registry API keys; only publish metadata and public identifiers.
- Lock holders identify the CI run or user via a non-sensitive holder string.

## Hosting Options

| Tier | Use Case | Notes |
|------|----------|-------|
| Managed Cloud | Teams without infrastructure | Beduality-hosted, account-based billing |
| Self-Hosted | Enterprises / privacy requirements | Open-source server image, same API |
| Local-Only | Solo developers | Existing behavior preserved, no cloud needed |

## Implementation Plan

1. Define cloud API specification (OpenAPI).
2. Implement the cloud backend: lock service, state store, project registry.
3. Add `terracotta-cloud` client module to the Terracotta SDK.
4. Extend Gradle plugin and CLI with cloud configuration.
5. Update diff engine to consume remote state.
6. Add integration tests with a test double backend.
7. Deploy managed cloud sandbox for beta users.
8. Document setup, token scopes, and offline behavior.

## Migration Path

1. Design cloud API and data model.
2. Implement backend services.
3. Add client module and configuration.
4. Integrate locks into `terracottaApply`.
5. Integrate state sync into `terracottaPlan`.
6. Add self-hosting documentation.
7. Open managed cloud beta.

## Risks & Considerations

| Risk | Mitigation |
|------|------------|
| Cloud becomes a single point of failure | Make it optional; support offline mode and self-hosting |
| Lock TTL shorter than upload duration | Implement heartbeat / lock extension during long operations |
| Sensitive metadata exposure | Store only publish metadata; keep tokens and secrets local |
| Adoption friction | Free tier for small projects; simple `terracotta cloud init` command |

## Next Steps

1. Finalize the cloud API specification.
2. Set up the backend repository and CI.
3. Implement the lock service as the first vertical slice.
4. Build the Gradle plugin integration behind a feature flag.

## References

- [External YAML Configuration](./2025-07-external-yaml-config.md)
- [Authentication Workflows](./2025-07-authentication-workflows.md)
