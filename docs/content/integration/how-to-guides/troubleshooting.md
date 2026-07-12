# Troubleshooting Provider Integration

This guide helps you diagnose common problems when adding or running a provider.

## Authentication errors

### `MODRINTH_TOKEN` or `HANGAR_TOKEN` not found

Symptom: the plan or apply task fails with an authentication error.

1. Confirm the environment variable is set:
   ```bash
   echo $MODRINTH_TOKEN
   ```
2. Make sure the variable name matches the provider ID in uppercase with a `_TOKEN` suffix.
3. If you set the token in the same shell where you run Gradle, export it:
   ```bash
   export MODRINTH_TOKEN="your_token"
   ```
4. If you set it in `build.gradle.kts`, make sure it is read from the environment or a credentials file, not hard-coded.

## Project not found

Symptom: Terracotta reports that the project does not exist on the provider.

- Check that `projectId` matches the project slug or ID on the provider.
- For Hangar, create the project manually first — Hangar does not support project creation through the API.
- For Modrinth, Terracotta can create the project if the token has permission.

## Unsupported loader warnings

Symptom: Terracotta skips a loader with a warning.

- Hangar supports only `paper`, `velocity`, and `waterfall` loaders. `fabric`, `forge`, `quilt`, `neoforge`, and `sponge` are skipped.
- Modrinth supports most loaders. Check the loader identifier in the [Loaders reference](../../modules/core/reference/loaders.md).

## Partial apply failure

Symptom: Modrinth succeeds but Hangar fails, or vice versa.

Terracotta applies each provider independently. A failure in one provider does not roll back another. After fixing the failing provider, re-run:

```bash
./gradlew terracottaApply
```

Check both providers after a partial failure to confirm the final state.

## Plan output is empty

Symptom: `terracottaPlan` reports no operations.

1. Verify the JAR artifact is produced by your Gradle build.
2. Verify the version you are publishing is new — Terracotta skips versions that already exist on the provider.
3. Check that `terracotta.yml` is in the project root or configured through the DSL.

## Getting more help

- [Provider configuration reference](../reference/provider-configuration.md)
- [Config schema reference](../../modules/core/reference/config-schema.md)
- [Modrinth provider docs](../../modules/provider-modrinth/README.md)
- [Hangar provider docs](../../modules/provider-hangar/README.md)
