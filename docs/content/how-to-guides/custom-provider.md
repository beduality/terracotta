# Implementing a Custom Provider

Terracotta core is modular and does not depend on Modrinth. You can add support for other registries (e.g. CurseForge or Hangar) by implementing core interfaces in the SDK.

## 1. Implement `StateProvider`

The `StateProvider` interface is responsible for fetching the current project state from the remote registry:

```kotlin
package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.model.TerracottaProject

interface StateProvider {
    fun fetchProject(projectId: String): TerracottaProject?
}
```

## 2. Implement `RegistryProvider`

The `RegistryProvider` interface executes the operations emitted by the Diff Engine:

```kotlin
package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation

interface RegistryProvider {
    fun apply(projectId: String, operations: List<Operation>)
}
```

## 3. Run the Diff Engine

Use `DiffEngine` to compute differences between local configuration and the retrieved remote project state:

```kotlin
val localProject: TerracottaProject = ...
val remoteProject: TerracottaProject? = customStateProvider.fetchProject(localProject.id)

// Compute differences
val operations: List<Operation> = DiffEngine.diff(localProject, remoteProject)

// Apply changes
customRegistryProvider.apply(localProject.id, operations)
```
