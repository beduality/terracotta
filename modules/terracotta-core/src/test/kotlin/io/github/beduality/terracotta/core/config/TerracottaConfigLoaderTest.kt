package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            categories:
              primary:
                id: paper
                displayName: Paper
              additional:
                - id: utility
                  displayName: Utility
            license: MIT
            licenseUrl: https://github.com/example/my-plugin/blob/main/LICENSE
            gameVersions:
              - 1.21.8
              - 1.21.7
            loaders:
              - paper
            environment: server_only
            releaseType: release
            visibility: unlisted
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
        assertEquals(
            TerracottaProjectCategories(
                primary = TerracottaCategory("paper", "Paper"),
                additional = listOf(TerracottaCategory("utility", "Utility")),
            ),
            config.categories,
        )
        assertEquals("MIT", config.license)
        assertEquals("https://github.com/example/my-plugin/blob/main/LICENSE", config.licenseUrl)
        assertEquals(listOf("1.21.8", "1.21.7"), config.gameVersions)
        assertEquals(listOf("paper"), config.loaders)
        assertEquals("server_only", config.environment)
        assertEquals("release", config.releaseType)
        assertEquals("unlisted", config.visibility)
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
        assertNull(config.icon)
        assertNull(config.categories)
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

    @Test
    fun `returns empty config when file is empty`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText("")

        val config = TerracottaConfigLoader.load(file)

        assertNull(config.name)
        assertTrue(config.providers.isEmpty())
    }

    @Test
    fun `returns empty config when root is not a map`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText("- a list\n- not a map\n")

        val config = TerracottaConfigLoader.load(file)

        assertNull(config.name)
        assertTrue(config.providers.isEmpty())
    }

    @Test
    fun `throws when yaml is malformed`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText("name: \"unclosed string")

        assertThrows<Exception> {
            TerracottaConfigLoader.load(file)
        }
    }

    @Test
    fun `ignores non-list values for list fields`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            name: Plugin
            categories:
              primary: paper
            loaders: fabric
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals("Plugin", config.name)
        assertNull(config.categories)
        assertNull(config.loaders)
    }

    @Test
    fun `loads gallery section`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            gallery:
              - path: docs/assets/main.png
                title: Main inventory screen
                description: Shows the new GUI
                featured: true
                ordering: 0
              - path: docs/assets/config.png
                title: Configuration UI
                ordering: 1
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals(2, config.gallery?.size)
        val first = config.gallery?.firstOrNull()
        assertEquals("docs/assets/main.png", first?.imagePath)
        assertEquals("Main inventory screen", first?.title)
        assertEquals("Shows the new GUI", first?.description)
        assertEquals(true, first?.featured)
        assertEquals(0, first?.ordering)
    }

    @Test
    fun `loads gallery key field`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            gallery:
              - path: docs/assets/main.png
                key: main-shot
                title: Main inventory screen
              - path: docs/assets/config.png
                title: Configuration UI
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        val items = config.gallery ?: throw AssertionError("Expected gallery items")
        assertEquals(2, items.size)
        assertEquals("main-shot", items[0].key)
        assertEquals("docs/assets/main.png", items[0].imagePath)
        assertNull(items[1].key)
    }

    @Test
    fun `gallery defaults optional fields`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            gallery:
              - path: docs/assets/simple.png
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        val item = config.gallery?.single()
        assertEquals("docs/assets/simple.png", item?.imagePath)
        assertEquals("", item?.title)
        assertEquals("", item?.description)
        assertEquals(false, item?.featured)
        assertEquals(0, item?.ordering)
    }

    @Test
    fun `loads icon field`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            name: Icon Plugin
            icon: docs/assets/icon.png
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertEquals("Icon Plugin", config.name)
        assertEquals("docs/assets/icon.png", config.icon)
    }
}
