package io.github.beduality.terracotta.cli.command

import io.github.beduality.terracotta.cli.config.parseConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CliCommandTest {
    @Test
    fun `test config parser resolves structure and path relativity`(
        @TempDir tempDir: File,
    ) {
        // Create a dummy readme
        val readmeFile =
            File(tempDir, "README.md").apply {
                writeText("# My Test Plugin Description")
            }

        // Create a dummy terracotta.yaml
        val configFile =
            File(tempDir, "terracotta.yaml").apply {
                writeText(
                    """
                    project:
                      id: my-plugin
                      name: My Plugin Name
                      summary: A summary here
                    description: README.md
                    license: MIT
                    tags:
                      - paper
                    versions:
                      - version: 1.0.0
                        artifact: build/libs/my-plugin.jar
                        gameVersions:
                          - 1.20
                        loaders:
                          - paper
                          - spigot
                    """.trimIndent(),
                )
            }

        val project = parseConfig(configFile)

        assertEquals("my-plugin", project.id)
        assertEquals("My Plugin Name", project.name)
        assertEquals("# My Test Plugin Description", project.description)
        assertEquals("MIT", project.license)
        assertEquals(listOf("paper"), project.tags)
        assertEquals(1, project.versions.size)
        assertEquals("1.0.0", project.versions.first().version)
        assertEquals(listOf("paper", "spigot"), project.versions.first().loaders)
    }
}
