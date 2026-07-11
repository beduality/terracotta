package io.github.beduality.terracotta.core.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TerracottaConfigLoaderTest {
    @Test
    fun `returns empty config when file does not exist`(
        @TempDir tempDir: File,
    ) {
        val config = TerracottaConfigLoader.load(File(tempDir, "terracotta.yml"))

        assertNull(config.name)
        assertTrue(config.providers.isEmpty())
    }

    @Test
    fun `loads full terracotta configuration`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            name: My Plugin
            summary: Lightweight Paper plugin
            description: A useful plugin
            tags:
              - paper
              - utility
            license: MIT
            gameVersions:
              - 1.21.8
              - 1.21.7
            loaders:
              - paper
            environment: server_only
            releaseType: release
            changelog: Initial release
            convention:
              readme: terracotta
              changelog: keep-a-changelog
            providers:
              modrinth:
                projectId: my-plugin
                token: yaml-token
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals("My Plugin", config.name)
        assertEquals("Lightweight Paper plugin", config.summary)
        assertEquals("A useful plugin", config.description)
        assertEquals(listOf("paper", "utility"), config.tags)
        assertEquals("MIT", config.license)
        assertEquals(listOf("1.21.8", "1.21.7"), config.gameVersions)
        assertEquals(listOf("paper"), config.loaders)
        assertEquals("server_only", config.environment)
        assertEquals("release", config.releaseType)
        assertEquals("Initial release", config.changelog)
        assertEquals("terracotta", config.convention.readme)
        assertEquals("keep-a-changelog", config.convention.changelog)

        assertEquals(1, config.providers.size)
        val modrinth = config.providers["modrinth"]
        assertEquals("my-plugin", modrinth?.projectId)
        assertEquals("yaml-token", modrinth?.token)
    }

    @Test
    fun `loads partial configuration with defaults`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            name: Minimal Plugin
            providers:
              modrinth:
                projectId: minimal
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals("Minimal Plugin", config.name)
        assertNull(config.summary)
        assertNull(config.tags)
        assertNull(config.convention.readme)
        assertNull(config.convention.changelog)
        assertEquals("minimal", config.providers["modrinth"]?.projectId)
        assertNull(config.providers["modrinth"]?.token)
    }

    @Test
    fun `loads multiple providers`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            name: Multi-Platform Plugin
            providers:
              modrinth:
                projectId: my-modrinth
              hangar:
                projectId: my-hangar
                token: hangar-token
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals(2, config.providers.size)
        assertEquals("my-modrinth", config.providers["modrinth"]?.projectId)
        assertEquals("my-hangar", config.providers["hangar"]?.projectId)
        assertEquals("hangar-token", config.providers["hangar"]?.token)
    }
}
