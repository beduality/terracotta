# Compute a Diff

Generate the list of operations needed to make a remote registry project match the local project state.

## Preconditions

- A local `TerracottaProject` has been built.
- A `StateProvider` can fetch the remote state.

## Steps

1. Fetch the remote project.
2. Pass both states to `DiffEngine.diff`.

```kotlin
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.provider.StateProvider
import kotlinx.coroutines.runBlocking

val remote: TerracottaProject? = runBlocking {
    stateProvider.fetchProject("my-project")
}

val operations = DiffEngine.diff(local, remote)
operations.forEach { println(it.description) }
```

## Steps to apply the operations

1. Pass the operations to a `RegistryProvider`.

```kotlin
runBlocking {
    registryProvider.apply("my-project", operations)
}
```

## Outcome

You have a list of `Operation` objects describing the minimum changes required to synchronize the remote state with the local state.

## Variants

- If you need to normalize versions before upload, pass the operations through `OperationPreprocessor.process` first.

## See also

- [Operations Reference](../reference/operations.md)
- [Diff Engine Explanation](../explanation/diff-engine.md)
