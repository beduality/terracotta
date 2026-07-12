# Proposal: YAML Schema for IDEs

**Date**: 2026-07-11  
**Status**: Draft  
**Related**: External YAML Configuration, Config Validation

## Summary

Provide a machine-readable **JSON Schema** for `terracotta.yml` so IDEs can offer autocomplete, inline validation, and quick documentation. The schema will be published to [SchemaStore](https://www.schemastore.org/) and bundled with the project, and it will be regenerated from the Kotlin configuration model to stay in sync with the code.

## Problem Statement

Today, `terracotta.yml` is documented in prose at `docs/content/config/schema.md`. Authors must read the docs or copy examples to know which keys are valid, what values they accept, and which are required. This leads to:

1. **Typos in keys** that are only caught at runtime.
2. **Invalid values** such as wrong environment names or unsupported loader IDs.
3. **Slow onboarding** because users constantly switch between editor and documentation.
4. **Drift between code and docs** when the config model changes.

## Why

A JSON Schema gives IDEs everything they need to help authors directly inside `terracotta.yml`:

- **Autocomplete** on keys and enum values.
- **Validation** of required fields, types, and provider-specific sections.
- **Hover documentation** pulled from schema descriptions.
- **Red squiggles** for mistakes before Gradle is invoked.
- **Consistency** across JetBrains, VS Code, Vim, and any other editor that supports SchemaStore.

It also improves the project itself: once the schema is generated from the code, the docs and the runtime model stay aligned.

## Proposed Schema

The schema describes the same structure already used by `TerracottaConfig` and `TerracottaProviderConfig` in the core module, plus any new fields introduced by the External YAML Configuration and Config Validation work.

### Example

```yaml
$schema: "https://terracotta.dev/schema/terracotta-1.schema.json"

name: "My Plugin"
summary: "Lightweight Paper plugin"
description: "A useful plugin."
license: "MIT"
tags:
  - paper
  - utility
gameVersions:
  - "1.21.8"
loaders:
  - paper
environment: server_only
releaseType: release
changelog: "Initial release"

providers:
  modrinth:
    projectId: "my-plugin"
    token: "${MODRINTH_TOKEN}"
```

### Schema Outline

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://terracotta.dev/schema/terracotta-1.schema.json",
  "title": "Terracotta Configuration",
  "description": "Declarative configuration for the Terracotta project registry manager.",
  "type": "object",
  "required": ["name", "summary", "description", "license"],
  "properties": {
    "$schema": { "type": "string", "format": "uri" },
    "name": { "type": "string", "description": "Project display name" },
    "summary": { "type": "string", "description": "Short project summary" },
    "description": { "type": "string", "description": "Full project description" },
    "tags": {
      "type": "array",
      "items": { "type": "string" }
    },
    "license": { "type": "string", "description": "License name or SPDX identifier" },
    "gameVersions": {
      "type": "array",
      "items": { "type": "string" }
    },
    "loaders": {
      "type": "array",
      "items": {
        "type": "string",
        "enum": [
          "bukkit", "bungeecord", "fabric", "folia", "forge",
          "neoforge", "paper", "purpur", "quilt", "spigot",
          "sponge", "velocity", "waterfall"
        ]
      }
    },
    "environment": {
      "type": "string",
      "enum": ["client_only", "server_only", "universal"]
    },
    "releaseType": {
      "type": "string",
      "enum": ["release", "beta", "alpha"]
    },
    "changelog": { "type": "string" },
    "providers": {
      "type": "object",
      "additionalProperties": {
        "type": "object",
        "properties": {
          "projectId": { "type": "string" },
          "token": { "type": "string" }
        },
        "required": ["projectId"]
      }
    }
  }
}
```

## How It Will Be Published

### 1. SchemaStore Catalog

The canonical schema will be published to [SchemaStore](https://www.schemastore.org/json/):

- File: `src/schemas/json/terracotta.json` in the SchemaStore repository.
- Catalog entry: `api/json/catalog.json` maps `terracotta.yml` and `terracotta.yaml` file patterns to the schema URL.

This gives every editor with SchemaStore support automatic validation out of the box, with no user setup.

### 2. Self-Hosted Schema

We will also host versioned schemas under our own domain:

```
https://terracotta.dev/schema/terracotta-1.schema.json
https://beduality.github.io/terracotta/schema/terracotta-1.schema.json
```

The self-hosted schema is generated at build time and committed to `docs/assets/schema/`, then deployed with MkDocs.

### 3. Bundled Schema

A copy of the schema will be shipped inside the `terracotta-core` JAR at `schema/terracotta-1.schema.json`. This lets the CLI validate files locally even when offline and guarantees that the schema version matches the runtime version.

## How to Use

### No Configuration (Default)

If the user's IDE supports SchemaStore and the catalog entry is merged, opening any `terracotta.yml` or `terracotta.yaml` file automatically enables autocomplete and validation.

### Explicit `$schema`

Users can pin a specific schema version:

```yaml
$schema: "https://terracotta.dev/schema/terracotta-1.schema.json"
```

This is useful for:

- Reproducible validation in CI.
- Projects that want to lock the schema while upgrading Terracotta.
- Editors that do not use SchemaStore.

### CLI Validation

The CLI can validate against the bundled schema:

```bash
terracotta validate --schema
```

This will be implemented as part of the Config Validation work.

### Gradle Plugin

The Gradle plugin will report schema mismatches during configuration if `terracotta.yml` is present. Warnings will be logged for deprecated keys.

## How We Will Keep It Up to Date

### 1. Generate from Kotlin Models

The schema will be generated from the core configuration classes rather than maintained by hand. The generator reads:

- `TerracottaConfig`
- `TerracottaProviderConfig`
- New typed models for `TerracottaLoader`, `TerracottaEnvironment`, `TerracottaReleaseType`, etc.

A small Kotlin task in the build will produce the JSON Schema from the classes and their KDoc comments.

### 2. CI Enforcement

A CI job will:

1. Run the schema generator.
2. Compare the generated file against the committed copy in `docs/assets/schema/` and `src/main/resources/schema/`.
3. Fail the build if they differ.

This prevents merged code from silently diverging from the schema.

### 3. Versioned Schemas

Each schema will be versioned with the file name and a top-level `schemaVersion` field (when that field is introduced):

```
terracotta-1.schema.json
terracotta-2.schema.json
```

Old schema versions remain available for projects that have not migrated. New Terracotta versions continue to parse older schema versions using the existing resolution logic.

### 4. SchemaStore Updates

SchemaStore updates will be automated via a release workflow step:

1. After a release is tagged, the CI workflow opens a pull request to SchemaStore with the latest generated schema.
2. A maintainer reviews and merges the PR.

## Implementation Plan

1. Add typed model classes for `TerracottaLoader`, `TerracottaEnvironment`, and `TerracottaReleaseType` if they do not already exist.
2. Write a JSON Schema generator task that reads the core Kotlin models.
3. Generate the first schema and commit it to `docs/assets/schema/` and `src/main/resources/schema/`.
4. Add the SchemaStore catalog entry and submission PR.
5. Add a `terracotta validate` CLI command that uses the bundled schema.
6. Wire the Gradle plugin to emit warnings on schema mismatches.
7. Add CI checks that fail when the schema is out of sync with the code.
8. Update documentation with the `$schema` recommendation and supported IDE behavior.

## Migration Path

1. Introduce the schema generator behind a feature flag or Gradle task.
2. Generate the v1 schema and validate it against existing test fixtures.
3. Submit the SchemaStore PR.
4. Enable CI enforcement.
5. Add documentation and mention the schema in the next release notes.

## Risks & Considerations

| Risk | Mitigation |
|------|------------|
| SchemaStore PR review delay | Host the schema ourselves first; SchemaStore is a bonus |
| Generated schema too strict | Allow additional properties where the runtime is lenient; add `additionalProperties` flags carefully |
| Breaking schema changes | Versioned schemas and backward-compatible parsing |
| Maintenance burden | Generation from code plus CI enforcement |

## Next Steps

1. Confirm the exact set of configuration fields that should be in the v1 schema.
2. Implement the schema generator.
3. Generate and validate the first schema against sample `terracotta.yml` files.
4. Submit the SchemaStore PR.

## References

- [External YAML Configuration](./2025-07-external-yaml-config.md)
- [Config Validation](./TODO.md) (pending proposal)
- [SchemaStore](https://www.schemastore.org/)
