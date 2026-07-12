# Proposal: Provider Topological Sort and Execution Order

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Introduce first-class provider dependencies in Terracotta so that providers can declare which other providers must run before them. The execution engine will build a directed acyclic graph (DAG) from the configured providers and their dependencies, topologically sort it, and run providers in the correct order. This enables providers to depend on each other (for example, `terracotta-provider-github-release` depends on `terracotta-provider-git`) without users having to manually order their configuration or providers having to implement ad-hoc coordination.

## Problem Statement

Terracotta currently treats providers as independent. When multiple providers are configured, they can run in any order or in parallel, and each provider is expected to produce its own operations without relying on another provider's side effects.

The new `terracotta-provider-git` and `terracotta-provider-github-release` providers break this assumption. The GitHub Release provider needs the Git provider to have already created tags, committed files, and updated repository metadata before it can create a GitHub release. If the Git provider runs after the GitHub Release provider, or not at all, the release will fail or attach to a stale tag.

Without a formal dependency mechanism, users would have to rely on implicit behavior such as configuration order, which is fragile and undocumented. Provider authors would also have to implement their own coordination logic, duplicating effort and increasing complexity.

## Goals

1. Allow providers to declare dependencies on other providers via the provider SPI.
2. Run configured providers in a deterministic, topologically sorted order.
3. Detect dependency cycles and missing dependencies before executing any provider.
4. Preserve the ability to run independent providers in parallel.
5. Keep the user-facing configuration unchanged; dependencies are declared by the provider implementation, not by users.
6. Provide clear error messages when dependency requirements are violated.

## Proposed Changes

### 1. Add Dependency Declaration to the Provider SPI

Extend the provider SPI so that a provider can declare which other provider IDs it depends on. The dependency declaration is static and known at provider discovery time.

**Option A**: Add a method to `ProviderFactory`.

```kotlin
interface ProviderFactory {
    val id: String
    fun create(config: ProviderConfig): Provider
    
    /**
     * Provider IDs that must run before this provider.
     * The provider itself does not need to be listed.
     */
    fun dependencies(): List<String> = emptyList()
}
```

**Option B**: Add a method to `Provider`.

```kotlin
interface Provider {
    val id: String
    fun plan(context: ProviderContext): List<Operation>
    fun apply(context: ProviderContext): List<OperationResult>
    
    /**
     * Provider IDs that must run before this provider.
     */
    fun dependencies(): List<String> = emptyList()
}
```

**Recommendation**: Use **Option A** (`ProviderFactory.dependencies()`). Dependencies are a property of the provider type, not a specific instance, and they should be known before any provider is instantiated. This also avoids needing to instantiate providers just to determine execution order.

Example declarations:

- `ModrinthProviderFactory.dependencies()` → `emptyList()`
- `HangarProviderFactory.dependencies()` → `emptyList()`
- `GitProviderFactory.dependencies()` → `emptyList()`
- `GitHubReleaseProviderFactory.dependencies()` → `listOf("git")`

### 2. Build and Sort the Provider DAG

When `terracottaPlan` or `terracottaApply` runs, the execution engine performs the following steps before invoking any provider:

1. Collect all configured providers from `TerracottaConfig`.
2. For each configured provider, resolve its factory and read its declared dependencies.
3. Validate that every declared dependency is either:
   - also configured in the current run, or
   - a known provider ID that the execution engine can ignore (e.g., an optional dependency).
4. Build a DAG where nodes are configured providers and edges are `dependency -> dependent`.
5. Detect cycles. If a cycle exists, fail fast with a clear error listing the cycle.
6. Topologically sort the DAG.
7. Run providers in sorted order. Providers at the same depth (no path between them) may run in parallel.

**Example DAG**:

```
modrinth
hangar
git
    └── github-release
```

Execution order: `modrinth`, `hangar`, `git` (in any order among themselves), then `github-release`.

### 3. Update the Execution Engine

The existing execution engine likely iterates over providers in configuration order or a registered order. It needs to be updated to:

- Accept a sorted list of providers instead of an unordered list.
- Respect the sorted order when calling `plan()` and `apply()`.
- Continue to support parallel execution for independent providers.
- Optionally, batch providers by topological depth so that all providers at depth `n` complete before any provider at depth `n+1` starts.

### 4. Validation and Error Handling

Introduce a `ProviderDependencyValidator` that runs before the execution engine:

- **Missing dependency**: If provider `github-release` depends on `git` but `git` is not configured, the validator fails with an error explaining that `github-release` requires `git`.
  - *Optional mitigation*: Allow a dependency to be marked as optional, in which case the dependent provider runs but is responsible for handling the absence itself.
- **Cycle**: If `a` depends on `b` and `b` depends on `a`, the validator fails with the cycle path.
- **Unknown dependency**: If a provider declares a dependency on a provider ID that does not exist in the registry, the validator fails.
- **Self-dependency**: A provider cannot depend on itself.

### 5. Changes to `validateTerracottaConfig`

The `validateTerracottaConfig` task should also perform dependency validation so that configuration errors are caught during validation, not during `terracottaApply`. This includes checking for missing and cyclic dependencies.

### 6. Configuration Unchanged

Users do not declare dependencies in `terracotta.yml`. Dependencies are an implementation detail of the providers. Users simply configure whichever providers they need:

```yaml
providers:
  git:
    # ...
  github-release:
    # ...
```

The execution engine determines the order.

## Client / Execution Design

### Dependency Resolution Algorithm

```kotlin
fun resolveExecutionOrder(
    configuredProviders: List<ProviderFactory>,
    registry: ProviderRegistry,
): List<ProviderFactory> {
    val graph = configuredProviders.associateWith { factory ->
        factory.dependencies()
            .map { depId ->
                registry.find(depId)
                    ?: error("Unknown provider dependency: $depId")
            }
            .filter { it in configuredProviders }
    }

    return topologicalSort(graph)
}
```

The topological sort produces a stable ordering. For deterministic behavior, ties are broken by provider ID or by configuration order.

### Parallel Execution

Providers at the same topological depth can run in parallel. For example, `modrinth`, `hangar`, and `git` can all run concurrently, but `github-release` waits for `git` to finish.

### Dry-Run Mode

In `terracottaPlan` (dry-run) mode, the execution engine still performs dependency validation and topological sorting. It logs the planned execution order so users can verify dependencies before running `terracottaApply`.

## Migration Path

1. Add `dependencies()` to the `ProviderFactory` SPI with a default empty list for backward compatibility.
2. Implement `ProviderDependencyValidator` and `TopologicalSort` utilities in `terracotta-core`.
3. Update the execution engine to use the sorted provider order.
4. Update `validateTerracottaConfig` to call the dependency validator.
5. Add unit tests for:
   - simple dependency chains
   - parallel independent providers
   - missing dependencies
   - cycles
   - self-dependencies
   - optional dependencies (if supported)
6. Update `terracotta-provider-github-release` to declare `listOf("git")` as a dependency.
7. Update provider documentation and the multi-provider tutorial to explain that order is determined by dependencies, not configuration order.

## Benefits

1. **Correct ordering**: Providers like `github-release` can rely on `git` having already run.
2. **No manual coordination**: Users do not need to think about provider order in their configuration.
3. **Parallelism preserved**: Independent providers still run in parallel, improving performance.
4. **Fail fast**: Missing or cyclic dependencies are caught before any provider runs.
5. **Extensible**: New providers can declare dependencies without changing the core execution model.
6. **Reusable providers**: Providers can be designed as building blocks for higher-level providers.

## Risks & Considerations

1. **Dependency cycles**: A cycle makes it impossible to determine an execution order.
   - **Mitigation**: Detect cycles during validation and fail with a clear error message.

2. **Missing dependencies**: A dependent provider may be configured without its dependency.
   - **Mitigation**: Fail fast during validation. Support optional dependencies in the future if needed.

3. **Implicit ordering changes behavior**: If two providers previously ran in configuration order and now run in dependency order, the behavior may change for users who relied on the old order.
   - **Mitigation**: Document the change. Use a stable tie-breaking rule (e.g., configuration order) so independent providers still behave predictably.

4. **Performance impact**: Building and sorting the DAG adds a small overhead.
   - **Mitigation**: The number of providers is small (typically 2–5), so the overhead is negligible. Cache dependency results if necessary.

5. **Provider dependencies are static**: Dependencies cannot change based on configuration.
   - **Mitigation**: This is intentional for simplicity. Dynamic ordering can be addressed later if a real use case arises.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Add `dependencies()` to `ProviderFactory` SPI
3. 🔄 Implement dependency validator and topological sort in `terracotta-core`
4. 🔄 Update execution engine to use sorted provider order
5. 🔄 Update `validateTerracottaConfig` task
6. 🔄 Add unit tests for dependency resolution
7. 🔄 Apply dependency declarations to `terracotta-provider-github-release`
8. 🔄 Update documentation and tutorials

## References

- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Git Provider Proposal](2025-07-git-provider.md)
- [GitHub Release Provider Proposal](2025-07-github-provider.md)
