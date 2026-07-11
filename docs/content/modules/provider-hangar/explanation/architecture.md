# Hangar Provider Architecture

The Hangar provider translates between Terracotta's generic operations and the Hangar API.

## Components

- `HangarProviderFactory` — creates state and registry providers for the `hangar` provider ID.
- `HangarStateProvider` — fetches project and version data from Hangar and converts it to `TerracottaProject`.
- `HangarRegistryProvider` — applies `Operation` objects to Hangar by calling the appropriate API endpoints.
- `HangarClient` — HTTP client that handles JWT authentication, project updates, channel management, and version uploads.
- `HangarLoaderMapper` — maps Terracotta loader IDs to Hangar platform names.

## Mapping behavior

- Hangar projects must be created manually through the Hangar UI; `Operation.CreateProject` logs a warning instead of creating a project.
- `TerracottaReleaseType` maps to Hangar channels (`Release`, `Snapshot`).
- Loader IDs are mapped to Hangar platforms via `HangarLoaderMapper`.
- `CLIENT_ONLY` environments are ignored because Hangar is implicitly server-only.

See the [Provider Interfaces reference](../../core/reference/provider-interfaces.md) for the generic abstractions this provider implements.
