---
description: Design proposal for adding terracottaDestroy tasks that can delete a remote project or all its versions, with per-provider support.
---

# Destroy Task Proposal

## TL;DR

Add `terracottaDestroy` and per-provider `terracottaDestroy<Provider>` Gradle tasks.
A destroy operation deletes the remote project (or optionally all of its versions) on a registry.
The feature extends the provider SPI with an optional destructive capability and adds multiple safety guards so it cannot be triggered accidentally.

## Problem Statement

Terracotta currently supports planning and applying changes to remote registries, but there is no first-class way to remove what was published. Users who want to delete a project or all of its versions must log into each registry's web UI and do it manually.

This proposal gives users a single, auditable command path for destruction while keeping the project safe from accidental data loss.

## Goals

- Add `terracottaDestroy` aggregate task and per-provider `terracottaDestroy<Provider>` tasks (e.g. `terracottaDestroyModrinth`).
- Support deleting the entire remote project by default.
- Support deleting all versions of a project while keeping the project page itself.
- Reuse the existing token convention (`<PROVIDER>_TOKEN` or `provider.token`).
- Make destructive operations explicit and hard to run by mistake.
- Update provider interfaces and clients without duplicating `apply`/`plan` logic.
- Update documentation (`tasks.md`) and integration tests.

## Non-Goals

- Deleting an individual version is out of scope for the initial implementation; it can be added later without changing the task surface.
- Restoring deleted projects or versions is out of scope.
- Destroying multiple unrelated projects in one invocation is out of scope; destroy operates on the single project declared in `terracotta.yml` / the Gradle extension.

## Proposed Design

### Gradle Tasks

Register one aggregate task and one task per configured provider, mirroring the existing `terracottaPlan` / `terracottaApply` pattern in `TerracottaTaskRegistrar`.

- `terracottaDestroy` — depends on all per-provider destroy tasks.
- `terracottaDestroy<Provider>` — destroys the project on a single provider only.

Each destroy task accepts the same inputs as `TerracottaApplyTask` where relevant (`projectId`, `provider`, `token`) plus new options.

### Destroy Options

| Option | Default | Meaning |
|--------|---------|---------|
| `--force` / `-f` | `false` | Skip the interactive confirmation prompt. Required in non-interactive environments such as CI. |
| `--versions-only` | `false` | Delete every version instead of deleting the whole project. |
| `--dry-run` | `false` | Print what would be destroyed and exit without making remote calls. |

If the project does not exist on the registry, the task logs a clear message and succeeds (idempotent no-op).

### Provider SPI Changes

Add a new capability interface so existing providers that have not implemented destruction remain source-compatible:

```kotlin
interface DestructiveRegistryProvider {
    /** Deletes the remote project identified by [projectId]. */
    suspend fun deleteProject(projectId: String)

    /** Deletes every version of the remote project identified by [projectId]. */
    suspend fun deleteAllVersions(projectId: String)
}
```

`ProviderFactory` grows an optional factory method:

```kotlin
interface ProviderFactory {
    // existing members
    fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider?
}
```

Returning `null` means the provider does not support destruction yet; the destroy task fails fast with a helpful message.

`RegistryProvider` itself is **not** modified, because destruction is not a `diff`/`apply` operation and should not flow through the `Operation` sealed class.

### New Task Class

`TerracottaDestroyTask`:

- Inputs: `projectId`, `provider`, `token`, `force`, `versionsOnly`, `dryRun`.
- Task action:
  1. Locate the provider factory via `ServiceLoader`.
  2. Resolve the destructive provider; fail if unsupported.
  3. In dry-run mode, fetch remote state via `StateProvider` and print the project / versions that would be deleted.
  4. If not forced and running interactively, prompt for confirmation (`y/N`).
  5. Call `deleteAllVersions` or `deleteProject` on the destructive provider.

### Provider Implementation Notes

- **Modrinth**: add `DELETE /project/{id}` and `DELETE /version/{id}` (or bulk variant) to `ModrinthClient`; wrap them in `ModrinthDestructiveRegistryProvider`.
- **Hangar**: add the equivalent Hangar deletion endpoints to `HangarClient`; wrap them in `HangarDestructiveRegistryProvider`.
- Both implementations must pass the same auth token used by the non-destructive clients.

## API Sketch

```kotlin
// terracotta-core
interface DestructiveRegistryProvider {
    suspend fun deleteProject(projectId: String)
    suspend fun deleteAllVersions(projectId: String)
}

interface ProviderFactory {
    val id: String
    fun createStateProvider(token: String?): StateProvider
    fun createRegistryProvider(token: String?): RegistryProvider
    fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider? = null
}

// terracotta-gradle-plugin
abstract class TerracottaDestroyTask : DefaultTask() {
    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val provider: Property<String>

    @get:Input
    @get:Optional
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    abstract val versionsOnly: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val dryRun: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val force: Property<Boolean>

    @TaskAction
    fun destroy() = runBlocking { /* ... */ }
}
```

## Task Registration

`TerracottaTaskRegistrar` is extended alongside the existing `allPlanTasks` / `allApplyTasks` collections:

```kotlin
val allDestroyTasks = mutableListOf<Any>()

extension.providers.all { providerExt ->
    // ... existing plan/apply registration ...

    val providerDestroyTask = project.tasks.register(
        "terracottaDestroy${providerId.replaceFirstChar(Char::titlecase)}",
        TerracottaDestroyTask::class.java,
    ) { task ->
        task.setDescription("Destroys the project on $providerId")
        task.group = "terracotta"
        task.projectId.set(providerExt.projectId)
        task.provider.set(providerId)
        task.token.set(providerExt.token)
    }
    allDestroyTasks.add(providerDestroyTask)
}

project.tasks.register("terracottaDestroy") {
    it.description = "Destroys the project on all configured providers"
    it.group = "terracotta"
    it.dependsOn(allDestroyTasks)
}
```

## Safety and User Experience

1. **Opt-in by default**: `terracottaDestroy` requires `--force` in non-interactive environments and prompts for confirmation otherwise.
2. **Dry-run first**: Users can run `./gradlew terracottaDestroy --dry-run` to inspect impact before applying it.
3. **Idempotent no-op**: If the project is already absent, the task reports `Project <id> not found on <provider>; nothing to destroy` and succeeds.
4. **Provider capability check**: Fails fast if a provider has not implemented `DestructiveRegistryProvider`.
5. **Per-provider isolation**: The aggregate task depends on per-provider tasks, so a failure in one provider does not cascade to others.

## Testing Strategy

- Add a contract test in `terracotta-core` asserting that a null `createDestructiveRegistryProvider` is handled gracefully.
- Add integration tests in `terracotta-gradle-plugin` verifying:
  - `terracottaDestroy` and `terracottaDestroyModrinth` are registered.
  - `--dry-run` does not call destructive providers.
  - Missing `--force` in non-interactive mode fails the build.
  - A missing project results in a no-op.
- Add provider tests using the existing fake HTTP clients to verify:
  - `deleteProject` sends the expected DELETE request.
  - `deleteAllVersions` iterates versions and deletes each one.

## Documentation Updates

- Update `docs/content/modules/gradle-plugin/reference/tasks.md` with `terracottaDestroy` and per-provider destroy tasks.
- Add a warning block about destructive operations and the `--force` requirement.
- Update the Modrinth and Hangar provider tutorials to mention deletion support and any registry-specific caveats.

## Open Questions

1. Should `--versions-only` be renamed to `--keep-project` to be clearer?
2. Should we add a `--yes` alias for `--force` to match common CLI conventions?
3. Should `terracottaDestroy` require an explicit allow-list of providers in the extension to prevent accidental multi-registry deletion?
4. Do Modrinth and Hangar expose stable project-deletion endpoints for API keys, or do they require session cookies / special scopes? This affects whether the default mode is project deletion or versions-only deletion.
5. Should failed deletions be partially rolled back? Most registries do not support undelete, so the answer is likely no, but the error message should list what succeeded before the failure.

## Risks

- **Accidental data loss**: Mitigated by the `--force` / prompt requirement and `--dry-run`.
- **Provider API limitations**: Some registries may not expose deletion endpoints. We can ship provider-specific warnings and fall back to a documented unsupported-provider message.
- **CI misuse**: Mitigated by requiring `--force`; CI pipelines that do not pass it will fail rather than destroy.
