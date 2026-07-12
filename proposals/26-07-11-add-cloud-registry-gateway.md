# Proposal: Terracotta Registry Gateway

**Date**: 2026-07-11  
**Status**: Draft  
**Related**: Terracotta Cloud, Authentication Workflows, Hangar Provider, CurseForge Provider

## Summary

Introduce the **Terracotta Registry Gateway**, a hosted distribution layer that removes the manual work of setting up and maintaining registry accounts. Users configure their project once in Terracotta; the Gateway automatically provisions projects on supported registries, rotates API credentials, and publishes versions on their behalf.

## Problem Statement

Publishing a Minecraft project to multiple registries today requires every author to:

1. Create and verify separate accounts on Modrinth, CurseForge, Hangar, and others.
2. Generate, scope, and securely store API tokens for each platform.
3. Map local project metadata to each registry's unique data model.
4. Handle authentication edge cases such as JWT refresh, rate limits, and token rotation.

This raises the barrier to entry for new authors, increases the risk of leaked credentials, and scatters project ownership across platforms.

## Goals

- Let users publish to multiple registries without creating accounts on every platform.
- Centralize credential lifecycle management (creation, rotation, revocation).
- Provide a unified API and configuration surface for distribution.
- Build on Terracotta Cloud for state, locks, and identity.
- Remain optional; users who already have registry accounts can keep using direct providers.

## Non-Goals

- Becoming a publisher itself (Terracotta does not host files or act as a store).
- Removing the existing direct provider implementations.
- Handling payouts, analytics, or moderation on behalf of registries.

## Proposed Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                      Terracotta Registry Gateway                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │   User Project   │  │ Registry Account │  │    Publish       │   │
│  │     Linking      │  │   Provisioning   │  │   Orchestrator   │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
└───────────────────────┬─────────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │Modrinth │   │CurseForge│   │ Hangar  │
   └─────────┘   └─────────┘   └─────────┘
```

The Gateway is built on top of Terracotta Cloud:

- **Terracotta Cloud** owns identity, locks, and publish state.
- **Registry Gateway** owns registry account provisioning, credential rotation, and distribution delegation.

## Core Concepts

### 1. Gateway Account

A single Terracotta account that represents an author or organization. One Gateway account can manage many projects and link them to many registries.

```kotlin
@Serializable
data class GatewayAccount(
    val id: String,
    val displayName: String,
    val email: String,
    val createdAt: Instant,
    val billingPlan: BillingPlan,
)
```

### 2. Registry Links

A registry link captures the relationship between a Terracotta project and a remote registry project.

```kotlin
@Serializable
data class RegistryLink(
    val id: String,
    val gatewayProjectId: String,
    val provider: String,           // e.g. "modrinth"
    val remoteProjectId: String,      // e.g. Modrinth project ID
    val remoteUrl: String,
    val createdAt: Instant,
    val status: LinkStatus,
)

enum class LinkStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DISCONNECTED,
}
```

### 3. Delegated Credentials

The Gateway creates and stores registry API credentials on behalf of the user. Users never see the raw tokens unless they explicitly choose direct provider mode.

```kotlin
interface CredentialVault {
    suspend fun issueCredential(
        accountId: String,
        provider: String,
        scopes: List<String>
    ): CredentialLease

    suspend fun rotateCredential(leaseId: String)
    suspend fun revokeCredential(leaseId: String)
}

@Serializable
data class CredentialLease(
    val id: String,
    val provider: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val scopes: List<String>,
)
```

## User Flow

### Step 1: Link a Registry

```bash
terracotta gateway link modrinth
```

The Gateway:

1. Checks whether the user already has a linked Modrinth identity.
2. If not, initiates an OAuth or API-key delegation flow to create one.
3. Stores a scoped credential lease in the vault.

### Step 2: Configure the Project

```yaml
# terracotta.yaml
project:
  name: "My Plugin"
  summary: "A useful Paper plugin."

gateway:
  enabled: true
  projectId: ${TERRACOTTA_CLOUD_PROJECT_ID}
  token: ${TERRACOTTA_CLOUD_TOKEN}
  providers:
    - modrinth
    - hangar
```

### Step 3: Terracotta Applies Through the Gateway

```bash
./gradlew terracottaApply
```

The client sends the resolved project metadata and artifact URLs to the Gateway. The Gateway:

1. Acquires a cloud lock for the version.
2. Ensures each registry project exists (creating it if necessary).
3. Publishes to each linked registry using its stored credentials.
4. Records the result in Terracotta Cloud state.
5. Releases the lock.

## Registry Project Auto-Creation

When a project does not yet exist on a registry, the Gateway creates it using platform-specific defaults derived from the canonical Terracotta configuration.

| Field | Source | Example |
|-------|--------|---------|
| Title | `project.name` | "My Plugin" |
| Slug / ID | `project.id` or generated from name | "my-plugin" |
| Summary | `project.summary` | "A useful Paper plugin." |
| Description | `project.description` | Long-form description |
| License | `project.license` | "MIT" |
| Categories / Tags | `project.tags` + loader mapping | "paper", "utility" |
| Loaders | `project.loaders` | `["paper"]` |

Platform-specific creation endpoints:

- **Modrinth**: `POST /v2/project` (requires accepted terms)
- **CurseForge**: `POST /projects` or Core API equivalent
- **Hangar**: `POST /api/v1/projects` with owner organization

The Gateway stores the returned remote project ID in the `RegistryLink`.

## API Design

### Publish Endpoint

```http
POST /v1/gateway/publish
Authorization: Bearer <terracotta-cloud-token>
Content-Type: application/json

{
  "projectId": "terracotta-project-uuid",
  "version": "1.2.3",
  "metadata": { /* resolved Terracotta metadata */ },
  "artifacts": [
    {
      "fileName": "my-plugin-1.2.3.jar",
      "url": "https://.../my-plugin-1.2.3.jar",
      "checksum": "sha256:..."
    }
  ],
  "providers": ["modrinth", "hangar"]
}
```

Response:

```json
{
  "publishId": "pub-uuid",
  "status": "in_progress",
  "providerResults": {
    "modrinth": { "status": "pending", "remoteVersionId": null },
    "hangar": { "status": "pending", "remoteVersionId": null }
  }
}
```

### Status Endpoint

```http
GET /v1/gateway/publish/{publishId}
```

Returns the current state of the publish job and final remote URLs once complete.

## Credential Security

- Credentials are encrypted at rest and never logged.
- Each credential lease is scoped to the minimum required permissions.
- Automatic rotation before expiry.
- Users can revoke a registry link at any time from the Gateway dashboard.
- Support for both Gateway-managed credentials and user-supplied credentials (direct mode).

## Configuration Model

```kotlin
@Serializable
data class GatewayConfig(
    val enabled: Boolean = false,
    val endpoint: String = "https://gateway.terracotta.dev",
    val projectId: String? = null,
    val token: String? = null,
    val providers: List<String> = emptyList(),
    val autoCreateProjects: Boolean = true,
    val autoCreateAccounts: Boolean = true,
)
```

## Gradle / CLI Integration

```yaml
# terracotta.yaml
cloud:
  enabled: true
  projectId: ${TERRACOTTA_CLOUD_PROJECT_ID}
  token: ${TERRACOTTA_CLOUD_TOKEN}

gateway:
  enabled: true
  providers: [modrinth, curseforge, hangar]
  autoCreateProjects: true
```

```bash
# Link a new registry account
terracotta gateway link modrinth

# Run distribution through the Gateway
./gradlew terracottaApply

# Or use the CLI directly
terracotta apply --gateway
```

## Benefits

1. **Lower barrier to entry**: Authors publish everywhere from a single account.
2. **Credential hygiene**: Tokens are rotated automatically and never exposed.
3. **Consistency**: Canonical metadata is mapped to each registry by a tested, shared layer.
4. **Team scaling**: New team members inherit registry links through the Gateway project.
5. **Auditability**: All publishes flow through Terracotta Cloud state.

## Risks & Considerations

| Risk | Mitigation |
|------|------------|
| Registry ToS restrictions on delegation | Review each platform's terms; offer manual fallback |
| Gateway account compromise | Require MFA, short-lived tokens, scoped credentials |
| Registry API changes breaking auto-creation | Abstract per-platform logic; add conformance tests |
| User resistance to hosted credential storage | Offer self-hosted Gateway and direct-provider mode |
| Duplicate slugs / name collisions | Generate slugs with conflict detection; allow override |

## Implementation Plan

1. Finalize Gateway API contract.
2. Implement credential vault and encryption.
3. Implement registry account linking flows for Modrinth and Hangar.
4. Implement auto-creation logic for each provider.
5. Build publish orchestrator on top of Terracotta Cloud locks and state.
6. Add `gateway` block to YAML config and Gradle extension.
7. Add CLI commands: `gateway link`, `gateway status`, `gateway unlink`.
8. Write end-to-end tests with sandbox registries.
9. Document ToS considerations and setup guide.

## Migration Path

1. Build Gateway backend as an extension of Terracotta Cloud.
2. Add client support behind a feature flag.
3. Support Modrinth first as the reference provider.
4. Add Hangar and CurseForge auto-creation.
5. Launch closed beta with selected projects.
6. Remove feature flag and document self-hosting.

## Alternatives Considered

### Alternative 1: Direct Providers Only
**Pros**: Simpler, no hosted infrastructure, full user control.  
**Cons**: High friction for multi-platform publishing, credential management burden remains.

### Alternative 2: Gateway Creates Accounts Without User Consent
**Pros**: Fully automatic onboarding.  
**Cons**: Likely violates registry ToS, risks account bans, poor user trust.

### Alternative 3: Proxy All Uploads Through a Single Terracotta Account
**Pros**: Simplest implementation.  
**Cons**: Loses per-author identity, violates most registry policies.

**Chosen**: User-linked delegated accounts with explicit consent, credential scoping, and per-project auto-creation.

## Next Steps

1. Validate approach with registry ToS reviews.
2. Implement the Modrinth linking and auto-creation vertical slice.
3. Design the self-hosted Gateway deployment package.

## References

- [Terracotta Cloud](./2026-07-terracotta-cloud.md)
- [Authentication Workflows](./2025-07-authentication-workflows.md)
- [Hangar Provider](./2025-07-hangar-provider.md)
- [CurseForge Provider](./2025-07-curseforge-provider.md)
