# Implementing a Custom Provider

This guide shows how to add support for additional registries to Terracotta.

## Overview

Terracotta's modular design lets you implement support for any registry by implementing three core interfaces.

## What you'll build

- `MyRegistryProviderFactory`: Factory for creating providers
- `MyRegistryStateProvider`: Fetches project state from your registry
- `MyRegistryRegistryProvider`: Applies changes to your registry
- ServiceLoader registration for automatic discovery

## Prerequisites

- Terracotta Core SDK dependency
- Access to your registry's API documentation

## Step 1: Implement ProviderFactory

Create the factory that Terracotta uses to discover your providers.

```kotlin
package com.example.provider

import io.github.beduality.terracotta.core.provider.*

class MyRegistryProviderFactory : ProviderFactory {
    override val id: String = "my-registry"
    
    override fun createStateProvider(token: String?): StateProvider {
        return MyRegistryStateProvider(token)
    }
    
    override fun createRegistryProvider(token: String?): RegistryProvider {
        return MyRegistryRegistryProvider(token)
    }
}
```

## Step 2: Implement StateProvider

Fetch project data from your registry and translate it to `TerracottaProject`.

```kotlin
class MyRegistryStateProvider(private val token: String?) : StateProvider {
    override suspend fun fetchProject(projectId: String): TerracottaProject? {
        // Fetch from your registry API
        val response = myRegistryClient.fetchProject(projectId, token)
        
        return response?.let {
            TerracottaProject(
                id = it.id,
                name = it.name,
                summary = it.summary,
                description = it.description,
                versions = it.versions.map { version ->
                    TerracottaVersion(
                        version = version.versionId,
                        artifactPath = version.artifactPath,
                        gameVersions = version.gameVersions,
                        loaders = version.loaders,
                        environment = version.environment,
                        releaseType = version.releaseType,
                    )
                },
                tags = it.tags,
                license = it.license,
            )
        }
    }
}
```

## Step 3: Implement RegistryProvider

Apply operations to your registry.

```kotlin
class MyRegistryRegistryProvider(private val token: String?) : RegistryProvider {
    override suspend fun apply(projectId: String, operations: List<Operation>) {
        operations.forEach { operation ->
            when (operation) {
                is Operation.CreateProject -> {
                    myRegistryClient.createProject(operation.project, token)
                }
                is Operation.UploadVersion -> {
                    myRegistryClient.uploadVersion(operation.version, token)
                }
                is Operation.UpdateMetadata -> {
                    myRegistryClient.updateMetadata(
                        projectId = projectId,
                        name = operation.newName,
                        summary = operation.newSummary,
                        license = operation.newLicense,
                        token = token,
                    )
                }
                is Operation.UpdateDescription -> {
                    myRegistryClient.updateDescription(
                        projectId = projectId,
                        description = operation.newDescription,
                        token = token,
                    )
                }
                is Operation.UpdateTags -> {
                    myRegistryClient.updateTags(
                        projectId = projectId,
                        tags = operation.newTags,
                        token = token,
                    )
                }
            }
        }
    }
}
```

## Step 4: Register via ServiceLoader

Create `src/main/resources/META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`:

```
com.example.provider.MyRegistryProviderFactory
```

## Step 5: Use Your Provider

Add your provider module as a dependency and use it like any other:

```kotlin
val factory = MyRegistryProviderFactory()
val stateProvider = factory.createStateProvider(token)
val registryProvider = factory.createRegistryProvider(token)

val remoteProject = stateProvider.fetchProject(projectId)
val operations = DiffEngine.diff(localProject, remoteProject)
registryProvider.apply(projectId, operations)
```

## Testing

Test your provider by mocking the API client:

```kotlin
@Test
fun `fetches project correctly`() = runTest {
    val mockClient = MockMyRegistryClient()
    val provider = MyRegistryStateProvider(mockClient)
    
    val project = provider.fetchProject("test-id")
    
    assertEquals("Test Project", project?.name)
}
```

## Debugging

- Check your registry API documentation for correct endpoint paths
- Verify authentication token format and headers
- Log HTTP requests/responses for troubleshooting
- Test with a real registry account before integrating with Terracotta