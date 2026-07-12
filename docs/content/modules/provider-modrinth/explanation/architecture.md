# Modrinth Provider Architecture

The Modrinth provider translates between Terracotta's generic operations and the Modrinth API.

## Components

- `ModrinthProviderFactory` — creates state and registry providers, plus the `ModrinthProviderLogic`, for the `modrinth` provider ID.
- `ModrinthProviderLogic` — provider-specific logic combining identity loader mapping and stateful platform behavior; returned by `ModrinthProviderFactory.createProviderLogic()`.
- `ModrinthStateProvider` — fetches project and version data from Modrinth and converts it to `TerracottaProject`.
- `ModrinthRegistryProvider` — extends `BaseRegistryProvider` and applies supported `Operation` objects to Modrinth by calling the appropriate API endpoints. Core handles filtering and skipped-operation logging.
- `ModrinthClient` — thin HTTP client over the Modrinth REST API.
- Model classes (`ModrinthProject`, `ModrinthVersion`, etc.) — serializable DTOs used by the client.

## Mapping behavior

- `TerracottaReleaseType` maps to Modrinth `version_type` (`release`, `beta`, `alpha`).
- Each `TerracottaVersion` is uploaded as a Modrinth version with a single primary file.
- `Operation.CreateProject` creates a draft Modrinth project through the project creation endpoint.

See the [Provider Interfaces reference](../../core/reference/provider-interfaces.md) for the generic abstractions this provider implements.
