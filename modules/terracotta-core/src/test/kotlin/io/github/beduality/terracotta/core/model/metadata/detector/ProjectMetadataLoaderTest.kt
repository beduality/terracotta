package io.github.beduality.terracotta.core.detect

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectMetadataLoaderTest {
    @Test
    fun `detects README description and summary`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # My Plugin

            Short summary.

            Longer description.
            """.trimIndent(),
        )

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), ProjectMetadataSource())

        assertEquals(
            """
            # My Plugin

            Short summary.

            Longer description.
            """.trimIndent(),
            result.description,
        )
        assertEquals("Short summary.", result.summary)
    }

    @Test
    fun `detects license from LICENSE file`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "LICENSE").writeText("MIT License")

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), ProjectMetadataSource())

        assertEquals("MIT", result.license)
    }

    @Test
    fun `detects fabric loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText(
                """
                {
                  "schemaVersion": 1,
                  "id": "my-mod",
                  "environment": "server"
                }
                """.trimIndent(),
            )
        }

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), ProjectMetadataSource())

        assertEquals(listOf("fabric"), result.loaders)
        assertEquals(TerracottaEnvironment.SERVER_ONLY, result.environment)
    }

    @Test
    fun `detects forge loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText("modLoader=\"javafml\"".trimIndent())
        }

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), ProjectMetadataSource())

        assertEquals(listOf("forge"), result.loaders)
    }

    @Test
    fun `detects paper loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/paper-plugin.yml").apply {
            parentFile.mkdirs()
            writeText(
                """
                name: MyPlugin
                version: '1.0'
                """.trimIndent(),
            )
        }

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), ProjectMetadataSource())

        assertEquals(setOf("paper", "spigot", "bukkit"), result.loaders?.toSet())
    }

    @Test
    fun `source metadata takes precedence over detected files`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # Readme Name

            Readme summary.
            """.trimIndent(),
        )

        val source = ProjectMetadataSource(name = "Source Name", summary = "Source summary.", version = "1.0.0")
        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), source)

        assertEquals("Source Name", result.name)
        assertEquals("Source summary.", result.summary)
        assertEquals(TerracottaReleaseType.RELEASE, result.releaseType)
        assertEquals(
            """
            # Readme Name

            Readme summary.
            """.trimIndent(),
            result.description,
        )
    }

    @Test
    fun `detects beta release type from version`(
        @TempDir tempDir: File,
    ) {
        val source = ProjectMetadataSource(version = "1.0.0-beta.1")

        val result = ProjectMetadataLoader.load(ProjectFileCache(tempDir), source)

        assertEquals(TerracottaReleaseType.BETA, result.releaseType)
    }
}
