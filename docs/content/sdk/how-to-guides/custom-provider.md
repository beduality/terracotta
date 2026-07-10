# Implementing a Custom Provider

Terracotta core is modular and does not depend on Modrinth. You can add support for other registries (e.g. CurseForge or Hangar) by implementing core interfaces in the SDK.

## 1. Implement `ProviderFactory`

The `ProviderFactory` interface acts as a factory for creating state and registry providers for your custom registry:

```kotlin
package io.github.beduality.terracotta.core.provider

interface ProviderFactory {
    /** The unique identifier for this registry (e.g. "modrinth", "hangar") */
    val id: String

    /** Creates a state provider for this registry using the given auth token (may be null) */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry using the given auth token (may be null) */
    fun createRegistryProvider(token: String?): RegistryProvider
}
```

Then, register your `ProviderFactory` implementation as a Java service by creating a file at `src/main/resources/META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory` with the fully qualified name of your implementation class.

## 2. Implement `StateProvider`

The `StateProvider` interface is responsible for fetching the current project state from the remote registry:

```kotlin
package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.model.TerracottaProject

interface StateProvider {
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
```

## 3. Implement `RegistryProvider`

The `RegistryProvider` interface executes the operations emitted by the Diff Engine:

```kotlin
package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation

interface RegistryProvider {
    suspend fun apply(projectId: String, operations: List<Operation>)
}
```

## 4. Run the Diff Engine

Use `DiffEngine` to compute differences between local configuration and the retrieved remote project state:

```kotlin
import kotlinx.coroutines.runBlocking

val localProject: TerracottaProject = ...
val remoteProject: TerracottaProject? = runBlocking { customStateProvider.fetchProject(localProject.id) }

// Compute differences
val operations: List<Operation> = DiffEngine.diff(localProject, remoteProject)

// Apply changes
runBlocking { customRegistryProvider.apply(localProject.id, operations) }
```
