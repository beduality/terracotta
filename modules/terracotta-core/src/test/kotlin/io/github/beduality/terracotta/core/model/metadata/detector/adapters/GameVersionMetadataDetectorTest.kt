package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.config.ProjectMetadataResolver
import io.github.beduality.terracotta.core.config.TerracottaConfig
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GameVersionMetadataDetectorTest {
    @Test
    fun `detects game version from plugin yml api-version`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/plugin.yml").apply {
            parentFile.mkdirs()
            writeText(
                """
                name: MyPlugin
                version: '1.0'
                api-version: '1.20'
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20"), result?.gameVersions)
    }

    @Test
    fun `detects game version from paper-plugin yml api-version`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/paper-plugin.yml").apply {
            parentFile.mkdirs()
            writeText(
                """
                name: MyPaperPlugin
                version: '1.0'
                api-version: '1.20.1'
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `detects game version from fabric mod json`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText(
                """
                {
                  "schemaVersion": 1,
                  "id": "my-mod",
                  "depends": {
                    "minecraft": ">=1.20.1"
                  }
                }
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `detects game version from mods toml`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText(
                """
                modLoader="javafml"
                loaderVersion="[47,)"
                [[mods]]
                modId="myforge"
                version="1.0.0"
                [[dependencies.myforge]]
                    modId="minecraft"
                    mandatory=true
                    versionRange="[1.20.1]"
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `returns null when no game version can be detected`(
        @TempDir tempDir: File,
    ) {
        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `explicit gameVersions in config overrides detected values`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/plugin.yml").apply {
            parentFile.mkdirs()
            writeText(
                """
                name: MyPlugin
                version: '1.0'
                api-version: '1.20'
                """.trimIndent(),
            )
        }

        val config = TerracottaConfig(gameVersions = listOf("1.21"))
        val resolved = ProjectMetadataResolver(tempDir, config, ProjectMetadataSource()).resolve()

        assertEquals(listOf("1.21"), resolved.gameVersions)
    }

    @Test
    fun `detects snapshot game version from fabric mod json`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText(
                """
                {
                  "schemaVersion": 1,
                  "id": "my-mod",
                  "depends": {
                    "minecraft": "25w14a"
                  }
                }
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("25w14a"), result?.gameVersions)
    }

    @Test
    fun `detects release candidate game version from mods toml`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText(
                """
                modLoader="javafml"
                loaderVersion="[55,)"
                [[mods]]
                modId="myforge"
                version="1.0.0"
                [[dependencies.myforge]]
                    modId="minecraft"
                    mandatory=true
                    versionRange="[1.21.5-rc1]"
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.21.5-rc1"), result?.gameVersions)
    }

    @Test
    fun `detects multiple game versions from a version range`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText(
                """
                modLoader="javafml"
                loaderVersion="[47,)"
                [[mods]]
                modId="myforge"
                version="1.0.0"
                [[dependencies.myforge]]
                    modId="minecraft"
                    mandatory=true
                    versionRange="[1.20.1,1.20.2)"
                """.trimIndent(),
            )
        }

        val result = GameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1", "1.20.2"), result?.gameVersions)
    }

    private fun context(tempDir: File): ProjectMetadataContext = ProjectMetadataContext(ProjectFileCache(tempDir), ProjectMetadataSource())
}
