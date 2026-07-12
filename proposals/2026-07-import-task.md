---
description: Design proposal for adding terracottaImport tasks that generate a local terracotta.yml from an existing remote project.
---

# Import Task Proposal

## TL;DR

Add `terracottaImport` and per-provider `terracottaImport<Provider>` Gradle tasks.
An import operation reads the current state of an existing remote project and
scaffolds a local `terracotta.yml` file from it. This lets users adopt Terracotta
for projects that were already published manually.

## Problem Statement

Terracotta assumes the local `terracotta.yml` / Gradle DSL is the source of truth.
Users who already have a project on Modrinth, Hangar, or another registry must
manually translate the remote metadata into a local file before they can use
`terracottaPlan` or `terracottaApply`.

This proposal provides a first-class, auditable command that reverse-engineers a
Terracotta configuration from the remote state, lowering the barrier to adopt
Terracotta for existing projects.

## Goals

- Add `terracottaImport` aggregate task and per-provider `terracottaImport<Provider>`
  tasks (e.g. `terracottaImportModrinth`).
- Generate a `terracotta.yml` file from the remote project state.
- Preserve provider-specific project identifiers, tags, license, gallery, and
  supported versions.
- Reuse the existing token convention (`<PROVIDER>_TOKEN` or `provider.token`).
- Make the generated file deterministic and easy to review before it is written.
- Keep the task read-only by default; do not mutate the remote registry.
- Update provider interfaces and clients without duplicating `apply`/`plan` logic.
- Update documentation (`tasks.md`) and integration tests.

## Non-Goals

- Importing multiple unrelated projects in one invocation is out of scope; import
  operates on the single project identified on the command line or configured in the
  Gradle extension.
- Downloading version artifacts from the registry is out of scope; imported
  configurations reference local files via the existing `artifactFile` DSL.
- Merging imported data into an existing `terracotta.yml` is out of scope for the
  first iteration; the task fails if the target file already exists unless
  `--overwrite` is passed.
- Importing from registries that do not expose public project read APIs is out of
  scope.

## Proposed Design

### Gradle Tasks

Register one aggregate task and one task per configured provider, mirroring the
existing `terracottaPlan` / `terracottaApply` pattern in `TerracottaTaskRegistrar`.

- `terracottaImport` — depends on all per-provider import tasks.
- `terracottaImport<Provider>` — imports the project from a single provider only.

Each import task accepts the same provider inputs as `TerracottaApplyTask` where
relevant (`projectId`, `provider`, `token`) plus new options.

### Import Options

| Option | Default | Meaning |
|--------|---------|---------|
| `--output-file` / `-o` | `terracotta.yml` | Path to the file that will be generated. |
| `--overwrite` | `false` | Overwrite the output file if it already exists. |
| `--dry-run` | `false` | Print the generated YAML to the console instead of writing it. |
| `--include-versions` | `false` | Also import published versions as entries in the generated file. |

If the project does not exist on the registry, the task fails fast with a clear
message.

### Provider SPI Changes

Import reuses the existing `StateProvider.fetchProject(projectId)` method to fetch
remote state. No new provider capability is required; any provider that already
supports planning can be imported from. The task only needs a way to serialize the
fetched `TerracottaProject` back into a `TerracottaConfig`.

Add a small serializer in `terracotta-core`:

```kotlin
object TerracottaConfigSerializer {
    /** Serializes [project] into a YAML string that matches `terracotta.yml` schema. */
    fun serialize(project: TerracottaProject, providerId: String): String
}
```

The serializer is intentionally a plain formatter, not a new provider interface,
because the YAML schema is owned by `terracotta-core`.

### New Task Class

`TerracottaImportTask`:

- Inputs: `projectId`, `provider`, `token`, `outputFile`, `overwrite`, `dryRun`,
  `includeVersions`.
- Task action:
  1. Locate the provider factory via `ServiceLoader`.
  2. Fetch remote state via `StateProvider`.
  3. Fail if the project is not found.
  4. Convert the `TerracottaProject` into a `TerracottaConfig`.
  5. Render the config as YAML.
  6. In dry-run mode, print the YAML and exit.
  7. If the output file exists and `--overwrite` was not passed, fail fast.
  8. Write the YAML to the output file atomically.

### Provider Implementation Notes

- **Modrinth**: reuse `ModrinthStateProvider.fetchProject`; no client changes
  needed.
- **Hangar**: reuse `HangarStateProvider.fetchProject`; no client changes needed.
- Providers that cannot read public project state must already fail during
  `fetchProject`, which the import task surfaces naturally.

## API Sketch

```kotlin
// terracotta-core
object TerracottaConfigSerializer {
    fun serialize(config: TerracottaConfig): String
}

// terracotta-gradle-plugin
abstract class TerracottaImportTask : DefaultTask() {
    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val provider: Property<String>

    @get:Input
    @get:Optional
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    abstract val outputFile: Property<String>

    @get:Input
    @get:Optional
    abstract val overwrite: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val dryRun: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val includeVersions: Property<Boolean>

    @TaskAction
    fun importProject() = runBlocking { /* ... */ }
}
```

## Task Registration

`TerracottaTaskRegistrar` is extended alongside the existing `allPlanTasks`,
`allApplyTasks`, and `allDestroyTasks` collections:

```kotlin
val allImportTasks = mutableListOf<Any>()

extension.providers.all { providerExt ->
    // ... existing plan/apply/destroy registration ...

    val providerImportTask = project.tasks.register(
        "terracottaImport${providerId.replaceFirstChar(Char::titlecase)}",
        TerracottaImportTask::class.java,
    ) { task ->
        task.setDescription("Imports the project metadata from $providerId into terracotta.yml")
        task.group = "terracotta"
        task.projectId.set(providerExt.projectId)
        task.provider.set(providerId)
        task.token.set(providerExt.token)
    }
    allImportTasks.add(providerImportTask)
}

project.tasks.register("terracottaImport") {
    it.description = "Imports the project metadata from all configured providers into terracotta.yml"
    it.group = "terracotta"
    it.dependsOn(allImportTasks)
}
```

## Output Format

The generated `terracotta.yml` contains:

```yaml
name: My Awesome Mod
summary: A short tagline
description: |
  Full project description imported from the registry.
license: MIT
tags:
  - utility
  - multiplayer
gameVersions:
  - "1.21"
loaders:
  - paper
providers:
  modrinth:
    projectId: my-awesome-mod
```

When `--include-versions` is passed, a `versions` list is added with the most
recent published versions. Because artifact paths cannot be inferred from the
registry, each entry uses a placeholder path and logs a warning telling the user
to update it:

```yaml
versions:
  - version: "1.0.0"
    artifactPath: TODO_SET_ARTIFACT_PATH
    gameVersions:
      - "1.21"
    loaders:
      - paper
    releaseType: release
```

## Safety and User Experience

1. **Read-only by default**: import only reads remote state and writes a local file.
2. **Fail on existing file**: the task refuses to overwrite an existing
   `terracotta.yml` unless `--overwrite` is passed.
3. **Dry-run first**: users can run `./gradlew terracottaImport --dry-run` to
   review the generated YAML before writing it.
4. **Missing project**: if `fetchProject` returns null, the task fails with a
   message naming the provider and project ID.
5. **Provider parity**: the aggregate task depends on per-provider tasks, so a
   failure in one provider does not cascade to others.
6. **Review reminder**: after a successful write, the task logs a reminder to
   review the generated file before running `terracottaApply`.

## Testing Strategy

- Add unit tests in `terracotta-core` for `TerracottaConfigSerializer` covering:
  - Minimal project serialization.
  - Provider block serialization.
  - Version list serialization with placeholder artifact paths.
- Add integration tests in `terracotta-gradle-plugin` verifying:
  - `terracottaImport` and `terracottaImportModrinth` are registered.
  - `--dry-run` does not write a file.
  - Missing project fails the build.
  - Existing file without `--overwrite` fails the build.
- Add provider tests using the existing fake HTTP clients to verify that the task
  calls `fetchProject` and handles null / non-null results.

## Documentation Updates

- Update `docs/content/modules/gradle-plugin/reference/tasks.md` with
  `terracottaImport` and per-provider import tasks.
- Add a tutorial under `docs/content/modules/gradle-plugin/tutorials/` showing
  how to bootstrap a project from Modrinth or Hangar.
- Mention that generated files should be reviewed and that artifact paths must be
  set before `terracottaApply` can be used.

## Open Questions

1. Should the aggregate `terracottaImport` write one file per provider or a single
   merged file? A single file is simpler; per-provider files can be added later.
2. Should `--include-versions` include every version or only the latest stable one?
3. Should the task also scaffold the Gradle `terracotta` DSL block, or only the
   YAML file?
4. Should imported gallery images be downloaded locally, or should only their
   remote URLs be captured as comments?
5. Should unsupported fields from the registry (e.g. donation links, Discord
   invite) be emitted as YAML comments for manual review?

## Risks

- **Overwriting user files**: mitigated by the `--overwrite` guard and dry-run mode.
- **Sensitive tokens in generated files**: the serializer must never emit the
  provider token; import relies on environment variables or the Gradle DSL for
  secrets.
- **Incompleteness**: imported files are a starting point, not a finished
  configuration. Documentation must make this clear.
- **Provider API drift**: if `StateProvider.fetchProject` gains or loses fields,
  the serializer must be updated. Keeping the serializer in `terracotta-core`
  keeps this coupling explicit.
