# Proposal: Terracotta Cloud Preview / Canonical Staging Service

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Build a proprietary **Terracotta Cloud Preview** service that lets developers publish to a Terracotta-controlled staging environment and inspect exactly how their release will be rendered — both in Terracotta's canonical model and in the native form of each target platform (Modrinth, CurseForge, Hangar). The service is delivered through a web UI and an optional CLI, and is offered as part of the paid **Terracotta Cloud** plan.

## Problem Statement

Publishing to multiple platforms is high-risk and hard to preview:

- A single configuration mistake can produce different, unexpected results on each provider.
- Provider-native staging endpoints (e.g., Modrinth Staging) only show that provider's view; they do not show the canonical Terracotta representation or side-by-side diffs.
- There is no place to review metadata, files, dependencies, gallery items, and platform-specific mappings in one place before going live.
- Teams want to share a preview link with reviewers or run CI checks against a stable, known environment.

A Terracotta-owned preview service solves these problems without requiring real accounts or credentials for every provider.

## Goals

1. Provide a safe, non-public staging environment under Terracotta's control.
2. Render the canonical Terracotta project view and each provider's mapped payload side-by-side.
3. Accept publishes via web UI upload and CLI command.
4. Generate stable preview URLs that can be shared with team members.
5. Surface validation errors and warnings before any real publish occurs.
6. Offer the service as a paid Cloud plan feature with clear usage tiers.

## Relationship to Provider-Native Staging

This service is **not a replacement** for provider-native staging environments like [Modrinth Staging](./2025-07-staging-environments.md). Those remain the best free option for testing against a specific provider's real API.

Terracotta Cloud Preview occupies a different layer:

- **Cross-platform review**: one preview shows all configured providers side-by-side.
- **Canonical intent check**: verify that the canonical model matches what the author intended before seeing platform drift.
- **No per-provider credentials required**: reviewers and CI can preview without accounts on every target platform.
- **Team workflow**: shareable links, approvals, and comparison between previews.

Use provider-native staging for "does this provider accept my payload?" Use Cloud Preview for "is this what I want to publish everywhere, and do all platform views agree?"

## Proposed Changes

### 1. Service Overview

**Terracotta Cloud Preview** is a hosted service with three components:

- **Preview API**: Receives a Terracotta project bundle and returns a preview ID.
- **Preview Renderer**: Renders the canonical model and platform-specific mappings.
- **Web UI**: Browse previews, compare canonical vs. provider views, and approve/reject.
- **CLI**: Push a project to preview from a local workspace or CI pipeline.

The service does **not** forward data to real provider APIs. It only simulates and displays what would be sent.

### 2. Canonical vs. Platform Views

For every preview, the service computes and displays:

- **Canonical view**: the normalized `TerracottaProject`, files, dependencies, gallery, and categories as Terracotta understands them.
- **Provider views**: per-platform mapped payloads, e.g.:
  - Modrinth: project JSON, version JSON, file upload metadata.
  - CurseForge: upload metadata, category hierarchy, file manifest.
  - Hangar: project/version metadata, channel mapping, platform groups.

Diffs between views highlight where provider semantics diverge from the canonical model.

### 3. Preview Bundle Format

The CLI and web upload accept the same bundle:

```yaml
preview:
  project: terracotta.yaml
  files:
    - build/libs/*.jar
  gallery:
    - docs/images/hero.png
  readme: README.md
```

Or via CLI:

```bash
terracotta preview --bundle terracotta-bundle.yaml
```

The CLI resolves local paths, computes checksums, and streams the bundle to the Preview API.

### 4. Web UI Features

- **Project dashboard**: list recent previews for a workspace.
- **Preview detail page**:
  - Canonical project card.
  - Tabs for each configured provider.
  - Raw mapped JSON for each provider.
  - File inventory with sizes and checksums.
  - Validation results (errors/warnings).
- **Compare mode**: diff two previews.
- **Shareable link**: `https://cloud.terracotta.dev/preview/{previewId}`.
- **Approve action**: mark a preview as approved and optionally trigger a real publish later.

### 5. CLI Features

```bash
# Push current workspace to preview
terracotta preview

# Push a specific bundle
terracotta preview --bundle path/to/bundle.yaml

# Open browser after push
terracotta preview --open

# Run against a custom Cloud instance (for enterprise/self-hosted later)
terracotta preview --endpoint https://cloud.example.com
```

The CLI should integrate with existing authentication workflows.

### 6. Integration with Config Validation

The preview service runs the same `ConfigValidator` used locally. Previews with validation errors are still stored but flagged, so users can see exactly what would fail at real publish time.

### 7. Cloud Plan Tiers

Preview service is a paid Cloud feature:

- **Free / Open Source**: limited number of previews per month, public preview links, no team sharing.
- **Cloud Plan**: unlimited previews, private links, team workspaces, CI API tokens, retention policy.
- **Enterprise (future)**: self-hosted option, SSO, audit logs.

## Migration Path

1. Design Preview API contract and data model.
2. Implement canonical renderer and provider-specific mappers in the Cloud backend.
3. Build web UI for project list, preview detail, and compare views.
4. Add `terracotta preview` CLI command to the Terracotta CLI.
5. Integrate with authentication and workspace/organization model.
6. Add usage tracking and plan enforcement.
7. Document pricing, usage limits, and CI examples.

## Benefits

1. **Confidence before publish**: developers can see exactly what each platform will receive.
2. **Faster iteration**: no need to create real releases or dummy projects on every provider.
3. **Team review**: shareable preview links simplify design, legal, and QA review.
4. **Provider parity visibility**: side-by-side canonical vs. provider views surface mapping gaps early.
5. **Revenue stream**: a clear paid Cloud feature that complements the open-source core.

## Risks & Considerations

1. **Scope creep**: the service could grow into a full package registry.
   - **Mitigation**: Keep it strictly a preview/validation tool; defer hosting binaries long-term.

2. **Provider drift**: simulated provider payloads may diverge from real provider behavior over time.
   - **Mitigation**: Use the same mapper code that drives real publishes and update it in lockstep.

3. **Data privacy**: users upload project metadata and files to Terracotta's cloud.
   - **Mitigation**: Clear retention policies, encryption at rest/transit, and enterprise self-hosted option later.

4. **Cost**: hosting previews and storing files incurs infrastructure cost.
   - **Mitigation**: Tiered pricing, short default retention, and file-size limits.

5. **Authentication complexity**: the service needs accounts, tokens, and team/organization support.
   - **Mitigation**: Reuse or extend the authentication model from [Authentication Workflows](./2025-07-authentication-workflows.md).

## Next Steps

1. ✅ Draft proposal and scope
2. 🔄 Define Preview API contract
3. 🔄 Design Cloud data model and storage
4. 🔄 Implement canonical renderer
5. 🔄 Implement provider mappers in Cloud backend
6. 🔄 Build web UI MVP
7. 🔄 Add `terracotta preview` CLI command
8. 🔄 Integrate authentication and billing
9. 🔄 Write documentation and pricing page

## References

- [Authentication Workflows](./2025-07-authentication-workflows.md)
- [Config Validation](./2025-07-config-validation.md)
- [Staging Environments](./2025-07-staging-environments.md)
- [Narrow License](./2025-07-narrow-license.md)
- [Narrow Tags](./2025-07-narrow-tags.md)
