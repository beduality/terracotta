# Hangar Provider Architecture

The Hangar provider translates between Terracotta's generic operations and the Hangar API.

## Components

- `HangarProviderFactory` — creates state and registry providers, plus the `HangarProviderLogic`, for the `hangar` provider ID.
- `HangarProviderLogic` — provider-specific logic combining loader mapping and platform behavior; returned by `HangarProviderFactory.createProviderLogic()`.
- `HangarStateProvider` — fetches project and version data from Hangar and converts it to `TerracottaProject`.
- `HangarRegistryProvider` — extends `BaseRegistryProvider` and applies supported `Operation` objects to Hangar by calling the appropriate API endpoints. Core handles filtering and skipped-operation logging.
- `HangarClient` — HTTP client that handles JWT authentication, project updates, channel management, and version uploads.
- `HangarLoaderMapper` — maps Terracotta loader IDs to Hangar platform names.

## Mapping behavior

- Hangar projects must be created manually through the Hangar UI; `Operation.CreateProject` logs a warning instead of creating a project.
- `TerracottaReleaseType` maps to Hangar channels (`Release`, `Snapshot`).
- Loader IDs are mapped to Hangar platforms via `HangarLoaderMapper`.
- `CLIENT_ONLY` environments are ignored because Hangar is implicitly server-only.

See the [Provider Interfaces reference](../../core/reference/provider-interfaces.md) for the generic abstractions this provider implements.
