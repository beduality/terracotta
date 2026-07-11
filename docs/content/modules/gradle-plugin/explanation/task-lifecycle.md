# Task Lifecycle

The plugin registers two families of tasks:

- `terracottaPlan` and `terracottaApply` operate on all configured providers.
- `terracottaPlan<Provider>` and `terracottaApply<Provider>` operate on a single provider.

## Planning

A plan task:

1. Resolves effective metadata.
2. Builds a local `TerracottaProject` from the extension and the artifact file.
3. Fetches the remote project state via the provider's `StateProvider`.
4. Runs `DiffEngine.diff(local, remote)` to compute operations.
5. Prints each operation without applying it.

## Applying

An apply task runs the same steps and then invokes `RegistryProvider.apply(projectId, operations)`. Because the apply task depends on the corresponding plan task, the plan is always computed first.

## Caching

Both task types are annotated with Gradle's `@DisableCachingByDefault` because they make network calls. Running them always fetches the latest remote state.

For a complete list of task names and flags, see the [Tasks reference](../reference/tasks.md).
