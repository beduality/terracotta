# Implement a Custom Loader

In this tutorial you will implement a custom `TerracottaLoader` that detects a fictional mod platform called `example` and registers it with `TerracottaLoaderRegistry`.

By the end you will be able to run Terracotta against a project that contains an `example-mod.json` descriptor and see `example` listed among the detected loaders.

## Prerequisites

- A local copy of the Terracotta repository.
- JDK 17 or later.
- Familiarity with Kotlin classes and interfaces.

## Step 1: Create the loader implementation

Create a new Kotlin file in `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/model/loader/adapters/ExampleLoader.kt`:

```kotlin
package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class ExampleLoader : AbstractTerracottaLoader("example", "Example") {
    override fun detect(cache: ProjectFileCache): Boolean {
        return cache.read("src/main/resources/example-mod.json") != null
    }
}
```

## Step 2: Register the loader

Open `TerracottaLoaderRegistry.kt` and add `register(ExampleLoader())` inside `registerDefaults()`.

## Step 3: Write a test

Create `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/model/ExampleLoaderTest.kt`:

```kotlin
package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.loader.ExampleLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ExampleLoaderTest {
    @Test
    fun `detects example loader from descriptor`(@TempDir tempDir: File) {
        File(tempDir, "src/main/resources").mkdirs()
        File(tempDir, "src/main/resources/example-mod.json").writeText("{}")

        val cache = ProjectFileCache(tempDir)
        val loader = ExampleLoader()

        assertTrue(loader.detect(cache))
    }
}
```

## Step 4: Run the test

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.model.ExampleLoaderTest"
```

The test should pass.

## Step 5: Verify detection through the registry

Add a second test that exercises the registry:

```kotlin
@Test
fun `registry detects example loader`(@TempDir tempDir: File) {
    File(tempDir, "src/main/resources").mkdirs()
    File(tempDir, "src/main/resources/example-mod.json").writeText("{}")

    val detected = TerracottaLoaderRegistry.detectAll(ProjectFileCache(tempDir))

    assertTrue(detected.any { it.id == "example" })
}
```

Run the test again. It should also pass.

## Final result

You now have a working custom loader that:

- Detects projects containing `src/main/resources/example-mod.json`.
- Appears in `TerracottaLoaderRegistry.detectAll(...)`.
- Can be used by the metadata detectors and the Gradle plugin without further changes.

## Next steps

- Read the [Loader Hierarchy](../explanation/loader-hierarchy.md) explanation to understand when to declare a `parent` loader.
- See the [Add a New Loader](../how-to-guides/add-a-new-loader.md) how-to guide for runtime registration without editing core.
