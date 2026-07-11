package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoaderRegistry
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LoaderMetadataDetectorTest {
    @Test
    fun `detects fabric loader and environment`(
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

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("fabric"), result?.loaders)
        assertEquals(TerracottaEnvironment.SERVER_ONLY, result?.environment)
    }

    @Test
    fun `detects forge loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText("modLoader=\"javafml\"".trimIndent())
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("forge"), result?.loaders)
    }

    @Test
    fun `detects neoforge loader and inherits forge`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/mods.toml").apply {
            parentFile.mkdirs()
            writeText("modLoader=\"neoforge\"".trimIndent())
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(setOf("neoforge", "forge"), result?.loaders?.toSet())
    }

    @Test
    fun `detects paper loader and bukkit family environment`(
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

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(setOf("paper", "spigot", "bukkit"), result?.loaders?.toSet())
        assertEquals(TerracottaEnvironment.SERVER_ONLY, result?.environment)
    }

    @Test
    fun `returns null when no loaders are detected`(
        @TempDir tempDir: File,
    ) {
        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `registry allows custom loader registration`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "custom-loader.json").writeText("{}")

        val customLoader =
            object : AbstractTerracottaLoader("custom", "Custom") {
                override fun detect(cache: ProjectFileCache): Boolean = cache.read("custom-loader.json") != null
            }
        TerracottaLoaderRegistry.register(customLoader)

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertTrue(result?.loaders?.any { it == "custom" } == true)
    }

    @Test
    fun `detects quilt loader and inherits fabric`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/quilt.mod.json").apply {
            parentFile.mkdirs()
            writeText("{}")
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(setOf("quilt", "fabric"), result?.loaders?.toSet())
    }

    @Test
    fun `detects waterfall loader and inherits bungeecord`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/bungee.yml").apply {
            parentFile.mkdirs()
            writeText("name: MyPlugin")
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(setOf("waterfall", "bungeecord"), result?.loaders?.toSet())
    }

    @Test
    fun `detects bungeecord loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/bungee.yml").apply {
            parentFile.mkdirs()
            writeText("name: MyPlugin")
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertTrue(result?.loaders?.contains("bungeecord") == true)
    }

    @Test
    fun `detects velocity loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/velocity-plugin.json").apply {
            parentFile.mkdirs()
            writeText("{}")
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("velocity"), result?.loaders)
    }

    @Test
    fun `detects sponge loader`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/META-INF/sponge_plugins.json").apply {
            parentFile.mkdirs()
            writeText("{}")
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("sponge"), result?.loaders)
    }

    @Test
    fun `detects bukkit and spigot from plugin yml`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/plugin.yml").apply {
            parentFile.mkdirs()
            writeText(
                """
                name: MyPlugin
                version: '1.0'
                """.trimIndent(),
            )
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(setOf("bukkit", "spigot"), result?.loaders?.toSet())
        assertEquals(TerracottaEnvironment.SERVER_ONLY, result?.environment)
    }

    @Test
    fun `detects fabric universal environment`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "src/main/resources/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText(
                """
                {
                  "schemaVersion": 1,
                  "id": "my-mod",
                  "environment": "*"
                }
                """.trimIndent(),
            )
        }

        val result = LoaderMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaEnvironment.UNIVERSAL, result?.environment)
    }

    private fun context(tempDir: File): ProjectMetadataContext = ProjectMetadataContext(ProjectFileCache(tempDir), ProjectMetadataSource())
}
