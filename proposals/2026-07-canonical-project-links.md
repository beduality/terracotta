# Proposal: Canonical Project Links Model

**Date**: 2026-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a first-class `links` field to `TerracottaProject` and `TerracottaConfig` so users can declare project URLs once and have each provider map them to its native format. Today Terracotta has no place for homepage, source, issue tracker, wiki, community, or donation links, which forces every provider to leave them empty or guess.

## Problem Statement

`TerracottaProject` currently carries no link metadata beyond `licenseUrl` and `icon`. Users cannot express:

- A project homepage or website.
- A source code repository.
- An issue tracker.
- A wiki or documentation URL.
- A community chat link (Discord, etc.).
- Donation links.

This means every provider omits links or requires provider-specific overrides. Because the supported platforms have very different link models, we need a canonical representation that is rich enough to round-trip common data but simple enough to map cleanly.

## Research Findings

### Modrinth (Labrinth API v3/v2)

A project exposes a small set of fixed optional URL fields plus a structured donation list.

| Modrinth Field | Meaning | Example |
|----------------|---------|---------|
| `issues_url` | Issue tracker URL | `https://github.com/user/repo/issues` |
| `source_url` | Source code URL | `https://github.com/user/repo` |
| `wiki_url` | Wiki or documentation URL | `https://github.com/user/repo/wiki` |
| `discord_url` | Discord invite URL | `https://discord.gg/invite` |
| `donation_urls` | Array of `{id, platform, url}` | `[{id: "patreon", platform: "Patreon", url: "..."}]` |

Modrinth also supports an extensible `link_platform` concept (via `GET /v3/link_platform`) for additional platform keys such as `github`, `twitter`, `website`, etc., surfaced as a key/value map on project creation. The official API read/write endpoints use the fixed fields above, while creation accepts a broader `external_links` map.

Sources: [Modrinth - Get a project](https://docs.modrinth.com/api/operations/getproject/), [Modrinth - Modify a project](https://docs.modrinth.com/api/operations/modifyproject/), [Modrinth - Creating projects](https://modrinth-code.mintlify.app/creators/creating-projects)

### CurseForge (Core API v1)

CurseForge stores links in a single nested `links` object on the `Mod` schema. All fields are strings and all are optional.

| CurseForge Field | Meaning |
|------------------|---------|
| `links.websiteUrl` | Project website or homepage |
| `links.wikiUrl` | Wiki URL |
| `links.issuesUrl` | Issue tracker URL |
| `links.sourceUrl` | Source code URL |

CurseForge has **no** native fields for Discord, community chat, or donation links. The `ModLinks` schema only exposes the four URLs above, so any canonical model must gracefully drop unsupported link types when targeting CurseForge.

Source: [CurseForge REST API - ModLinks](https://docs.curseforge.com/rest-api/)

### Hangar (REST API v1/v2)

Hangar models project links as a list of **link sections**. Each section has a `title`, a `type` (`TOP` or `SIDEBAR`), and a list of `Link` objects with `id`, `name`, and `url`. This is far more flexible than the other two platforms: authors can define arbitrary groupings and labels.

```json
{
  "settings": {
    "links": [
      {
        "title": "Links",
        "type": "TOP",
        "links": [
          { "name": "Source", "url": "https://github.com/user/repo" },
          { "name": "Issues", "url": "https://github.com/user/repo/issues" },
          { "name": "Discord", "url": "https://discord.gg/invite" }
        ]
      }
    ]
  }
}
```

Hangar also has a deprecated `ProjectDonationSettings` object (`enable` + `subject`), but it is not a generic donation link list. Donation links should be treated as unsupported on Hangar until the platform provides a stable, documented replacement.

Source: [Hangar API OpenAPI spec](https://hangar.papermc.io/v3/api-docs/public)

### Lowest Common Denominator

| Concept | Modrinth | CurseForge | Hangar | Canonical Support |
|---------|----------|------------|--------|-------------------|
| Homepage / website | Via `link_platform` | `websiteUrl` | Link section | Yes |
| Source | `source_url` | `sourceUrl` | Link section | Yes |
| Issues | `issues_url` | `issuesUrl` | Link section | Yes |
| Wiki | `wiki_url` | `wikiUrl` | Link section | Yes |
| Community chat | `discord_url` | None | Link section | Yes (best-effort) |
| Donations | `donation_urls` | None | Deprecated | Yes (best-effort) |
| Custom links | `link_platform` map | None | Link sections | Yes (limited) |

## Proposed Design

### Add `links` to canonical models

Introduce `TerracottaProjectLinks` and `TerracottaDonationLink` in `terracotta-core`.

```kotlin
@Serializable
data class TerracottaProjectLinks(
    /** Homepage or project website. */
    val homepage: String? = null,
    /** Source code repository. */
    val source: String? = null,
    /** Issue tracker. */
    val issues: String? = null,
    /** Wiki or documentation. */
    val wiki: String? = null,
    /** Community chat / forum (e.g., Discord invite). */
    val community: String? = null,
    /** Donation links. */
    val donations: List<TerracottaDonationLink> = emptyList(),
    /** Additional provider-agnostic links keyed by a short identifier. */
    val other: Map<String, String> = emptyMap(),
)

@Serializable
data class TerracottaDonationLink(
    /** Donation platform identifier (e.g., `patreon`, `bmac`, `ko-fi`). */
    val platform: String,
    /** URL to the donation page. */
    val url: String,
)
```

Add the field to `TerracottaProject`:

```kotlin
data class TerracottaProject(
    // ... existing fields ...
    val links: TerracottaProjectLinks = TerracottaProjectLinks(),
    // ...
)
```

Add the same field to `TerracottaConfig` and `AbstractProjectMetadata` so it can be declared in `terracotta.yml`, provided by the Gradle DSL, and merged like other metadata.

### Gradle DSL sketch

```kotlin
terracotta {
    links {
        homepage = "https://example.com"
        source = "https://github.com/user/repo"
        issues = "https://github.com/user/repo/issues"
        wiki = "https://github.com/user/repo/wiki"
        community = "https://discord.gg/invite"
        donation("patreon", "https://www.patreon.com/user")
        donation("bmac", "https://www.buymeacoffee.com/user")
        other("twitter", "https://twitter.com/user")
    }
}
```

YAML equivalent:

```yaml
links:
  homepage: "https://example.com"
  source: "https://github.com/user/repo"
  issues: "https://github.com/user/repo/issues"
  wiki: "https://github.com/user/repo/wiki"
  community: "https://discord.gg/invite"
  donations:
    - platform: patreon
      url: "https://www.patreon.com/user"
    - platform: bmac
      url: "https://www.buymeacoffee.com/user"
  other:
    twitter: "https://twitter.com/user"
```

## Provider-Specific Mapping

### Modrinth Provider

| Canonical | Modrinth |
|-----------|----------|
| `source` | `source_url` |
| `issues` | `issues_url` |
| `wiki` | `wiki_url` |
| `community` | `discord_url` (if URL looks like a Discord invite) or entry in `link_platform` map |
| `donations` | `donation_urls` |
| `other` | `link_platform` key/value pairs |

`homepage` can be emitted as a `website` entry in the `link_platform` map if the platform key is known to be supported; otherwise it can be omitted from the write path with a log warning.

### CurseForge Provider

| Canonical | CurseForge |
|-----------|------------|
| `homepage` | `links.websiteUrl` |
| `source` | `links.sourceUrl` |
| `issues` | `links.issuesUrl` |
| `wiki` | `links.wikiUrl` |
| `community` | Dropped (not supported) |
| `donations` | Dropped (not supported) |
| `other` | Dropped (not supported) |

Dropped fields are logged at `INFO` during `UpdateMetadata` so users know why they did not appear.

### Hangar Provider

Map the canonical fields to a default `TOP` link section titled **"Links"`. Custom `other` entries are appended to the same section. Multiple sections can be supported later via provider-specific overrides; for this proposal, keep the mapping simple.

```kotlin
val defaultSection = LinkSection(
    title = "Links",
    type = "TOP",
    links = buildList {
        links.homepage?.let { add(Link(name = "Website", url = it)) }
        links.source?.let { add(Link(name = "Source", url = it)) }
        links.issues?.let { add(Link(name = "Issues", url = it)) }
        links.wiki?.let { add(Link(name = "Wiki", url = it)) }
        links.community?.let { add(Link(name = "Community", url = it)) }
        links.other.forEach { (name, url) -> add(Link(name = name.replaceFirstChar { it.uppercase() }, url = url)) }
    }
)
```

Donations are **not** mapped because Hangar's donation setting is deprecated and not a generic link list.

## Migration Path

1. Create `TerracottaProjectLinks` and `TerracottaDonationLink` in `terracotta-core`.
2. Add `links: TerracottaProjectLinks` to `TerracottaProject`, `TerracottaConfig`, and `AbstractProjectMetadata`/`ProjectMetadata`.
3. Update the Gradle plugin DSL (`TerracottaExtension` and `TerracottaProjectSpec`) to expose a `links { ... }` block.
4. Implement read/write mapping in the Modrinth provider.
5. Implement write mapping in the Hangar provider.
6. Implement write mapping in the CurseForge provider (design only until CurseForge provider is implemented).
7. Update YAML serialization tests and add round-trip tests for each provider.
8. Update `docs/content/modules/core/reference/models.md` and `config-schema.md`.

## API Sketch

```kotlin
// Core model
@Serializable
data class TerracottaProjectLinks(
    val homepage: String? = null,
    val source: String? = null,
    val issues: String? = null,
    val wiki: String? = null,
    val community: String? = null,
    val donations: List<TerracottaDonationLink> = emptyList(),
    val other: Map<String, String> = emptyMap(),
)

@Serializable
data class TerracottaDonationLink(
    val platform: String,
    val url: String,
)

// Extension of existing metadata
abstract class AbstractProjectMetadata(
    // ... existing fields ...
    override val links: TerracottaProjectLinks? = null,
) : ProjectMetadata

// Extension of existing project
data class TerracottaProject(
    // ... existing fields ...
    val links: TerracottaProjectLinks = TerracottaProjectLinks(),
)

// Extension of existing config
data class TerracottaConfig(
    // ... existing fields ...
    val links: TerracottaProjectLinks? = null,
)
```

## Testing Strategy

- **Unit tests**: `TerracottaProjectLinks` serialization/deserialization, merge semantics in `AbstractProjectMetadata`, and validation that `other` is ordered consistently.
- **Provider tests**: 
  - Modrinth: assert that canonical fields map to the correct JSON keys and donation URLs round-trip.
  - Hangar: assert that canonical fields are converted into a single `TOP` link section with expected names.
  - CurseForge: assert that only the four supported fields are mapped and unsupported fields are dropped without error.
- **Integration tests**: Extend `TerracottaPluginIntegrationTest` to declare links in the Gradle DSL and verify they appear in the generated `terracotta.yml` / resolved project.
- **Manual verification**: Create a test project on each provider, run `terracottaApply`, and inspect the published links.

## Documentation Updates

- `docs/content/modules/core/reference/models.md`: add a `TerracottaProjectLinks` section and update the `TerracottaProject` table.
- `docs/content/modules/core/reference/config-schema.md`: add the `links` block to the YAML reference.
- `docs/content/modules/provider-modrinth/reference/api.md`: document how each canonical field maps to Modrinth.
- `docs/content/modules/provider-hangar/reference/api.md`: document the default "Links" section mapping.
- `docs/content/modules/provider-curseforge/reference/api.md` (when created): document the supported subset.
- Add a migration note in `CHANGELOG.md`.

## Open Questions

1. Should `other` be a `Map<String, String>` or a typed list of `TerracottaCustomLink(name, url)`? A map is simpler for YAML but loses display-name flexibility. Typed links are better for Hangar but overkill for Modrinth/CurseForge.
2. Should `homepage` be the same as `website`, or should we support both? Modrinth uses `link_platform` keys, CurseForge uses `websiteUrl`, and Hangar uses a free label. A single `homepage` field is the least surprising name.
3. Should donation `platform` values be an enum or free string? Modrinth's list is controlled but grows over time; a free string with validation against the platform list is more maintainable.
4. Do we need provider-specific overrides (e.g., a different Discord URL for Modrinth vs. Hangar)? This proposal keeps the model provider-agnostic; provider-specific overrides can be added later if real use cases appear.

## Risks & Considerations

1. **Platform asymmetry**: Hangar uses labeled link sections, Modrinth uses fixed fields plus a map, and CurseForge uses a fixed four-field object. The canonical model cannot preserve every nuance of every platform.
   - **Mitigation**: Define clear mapping rules, document dropped fields, and allow provider-specific overrides in a later proposal if needed.

2. **Breaking change**: Adding `links` to `TerracottaProject` changes the constructor signature.
   - **Mitigation**: Use a default value (`TerracottaProjectLinks()`) so existing callers and serialized payloads continue to work. Bump the minor version and document the new field.

3. **Donation platform validation**: Modrinth validates donation platform IDs. If a user supplies an unknown ID, the provider will reject the update.
   - **Mitigation**: Validate `donation.platform` against the known Modrinth donation platform list during planning and emit a clear error before making the network call.

4. **Hangar link section title conflicts**: If a user later adds provider-specific Hangar overrides, multiple sections titled "Links" could collide.
   - **Mitigation**: Treat the default section as provider-managed; reserve the title "Links" for the canonical mapping and allow overrides only through a dedicated `hangar.links` configuration block.

## Next Steps

1. ✅ Complete research on Modrinth, CurseForge, and Hangar link models.
2. 🔄 Create `TerracottaProjectLinks` and `TerracottaDonationLink`.
3. 🔄 Add `links` to `TerracottaProject`, `TerracottaConfig`, and `ProjectMetadata`.
4. 🔄 Implement Gradle DSL `links { ... }` block.
5. 🔄 Implement Modrinth read/write mapping.
6. 🔄 Implement Hangar write mapping.
7. 🔄 Implement CurseForge write mapping (when provider is available).
8. 🔄 Add tests and documentation.

## References

- [Modrinth API - Get a project](https://docs.modrinth.com/api/operations/getproject/)
- [Modrinth API - Modify a project](https://docs.modrinth.com/api/operations/modifyproject/)
- [Modrinth Docs - Creating projects](https://modrinth-code.mintlify.app/creators/creating-projects)
- [Modrinth API - Donation platforms](https://docs.modrinth.com/api/operations/donationplatformlist/)
- [CurseForge REST API](https://docs.curseforge.com/rest-api/)
- [Hangar API OpenAPI spec](https://hangar.papermc.io/v3/api-docs/public)
