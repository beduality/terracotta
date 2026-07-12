# Implement a Custom Metadata Detector

In this tutorial you will implement a `ProjectMetadataDetector` that reads a fictional `project.toml` file and contributes the project name and license to the resolved metadata.

By the end you will be able to resolve metadata from a directory containing `project.toml` and see the detected values in the output.

## Prerequisites

- A local copy of the Terracotta repository.
- JDK 17 or later.
- You have read [Implement a Custom Loader](implementing-a-custom-loader.md) or understand how `ProjectFileCache` works.

## Step 1: Create the detector

Create `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/model/metadata/detector/adapters/ProjectTomlDetector.kt`:

```kotlin
package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata

class ProjectTomlDetector : ProjectMetadataDetector {
    override fun detect(context: ProjectMetadataContext): TerracottaProjectMetadata? {
        val content = context.cache.read("project.toml") ?: return null

        val name = parseValue(content, "name") ?: return null
        val license = parseValue(content, "license")

        return TerracottaProjectMetadata(
            name = name,
            license = license,
        )
    }

    private fun parseValue(content: String, key: String): String? {
        val regex = Regex("""^$key\\s*=\\s*"([^"]+)""", RegexOption.MULTILINE)
        return regex.find(content)?.groupValues?.get(1)
    }
}
```

## Step 2: Register the detector via ServiceLoader

Create the service file:

```
modules/terracotta-core/src/main/resources/META-INF/services/io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataDetector
```

Add the fully qualified class name:

```
io.github.beduality.terracotta.core.model.metadata.detector.adapters.ProjectTomlDetector
```

## Step 3: Write a test

Create `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/model/metadata/detector/adapters/ProjectTomlDetectorTest.kt`:

```kotlin
package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.detector.ProjectMetadataLoader
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectTomlDetectorTest {
    @Test
    fun `detects name and license from project.toml`(@TempDir tempDir: File) {
        File(tempDir, "project.toml").writeText(
            """
            name = "My Project"
            license = "MIT"
            """.trimIndent(),
        )

        val metadata = ProjectMetadataLoader.load(
            ProjectFileCache(tempDir),
            ProjectMetadataSource(),
        )

        assertEquals("My Project", metadata.name)
        assertEquals("MIT", metadata.license)
    }
}
```

## Step 4: Run the test

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.model.metadata.detector.adapters.ProjectTomlDetectorTest"
```

The test should pass.

## Final result

You now have a detector that:

- Reads `project.toml` when it exists.
- Contributes `name` and `license` to the merged metadata.
- Is discovered automatically by `ProjectMetadataLoader` through Java's `ServiceLoader`.

## Next steps

- Read [Metadata Resolution](../explanation/metadata-resolution.md) to learn how detected values interact with explicit configuration.
- See [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md) for the full resolution workflow.
