package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectMetadataResolverTest {
    @Test
    fun `config values take precedence over detected values`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # Readme Name

            Readme summary.
            """.trimIndent(),
        )
        File(tempDir, "LICENSE").writeText("MIT License")

        val config =
            TerracottaConfig(
                name = "Config Name",
                summary = "Config summary",
                license = "Apache-2.0",
                licenseUrl = "https://config.example.com/LICENSE",
            )
        val resolver = ProjectMetadataResolver(tempDir, config, ProjectMetadataSource())

        val result = resolver.resolve()

        assertEquals("Config Name", result.name)
        assertEquals("Config summary", result.summary)
        assertEquals("Apache-2.0", result.license)
        assertEquals("https://config.example.com/LICENSE", result.licenseUrl)
    }

    @Test
    fun `detected values fill missing config fields`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # Readme Name

            Readme summary.

            Longer description.
            """.trimIndent(),
        )
        File(tempDir, "LICENSE").writeText("MIT License")
        File(tempDir, "src/main/resources/fabric.mod.json").apply {
            parentFile.mkdirs()
            writeText("{}")
        }

        val config = TerracottaConfig()
        val resolver = ProjectMetadataResolver(tempDir, config, ProjectMetadataSource())

        val result = resolver.resolve()

        assertEquals("Readme summary.", result.summary)
        assertEquals("MIT", result.license)
        assertTrue(result.loaders.contains("fabric"))
    }

    @Test
    fun `source values are used when config and detection are empty`(
        @TempDir tempDir: File,
    ) {
        val config = TerracottaConfig()
        val source = ProjectMetadataSource(name = "Source Name", summary = "Source summary", version = "1.0.0")
        val resolver = ProjectMetadataResolver(tempDir, config, source)

        val result = resolver.resolve()

        assertEquals("Source Name", result.name)
        assertEquals("Source summary", result.summary)
        assertEquals(TerracottaReleaseType.RELEASE, result.releaseType)
    }

    @Test
    fun `detects changelog from CHANGELOG for source version`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "CHANGELOG.md").writeText(
            """
            # Changelog

            ## [1.2.0]

            ### Fixed
            - a bug
            """.trimIndent(),
        )

        val config = TerracottaConfig()
        val source = ProjectMetadataSource(version = "1.2.0")
        val resolver = ProjectMetadataResolver(tempDir, config, source)

        val result = resolver.resolve()

        assertEquals("### Fixed\n- a bug", result.changelog)
    }

    @Test
    fun `applies defaults when no values are available`(
        @TempDir tempDir: File,
    ) {
        val config = TerracottaConfig()
        val resolver = ProjectMetadataResolver(tempDir, config, ProjectMetadataSource())

        val result = resolver.resolve()

        assertEquals("", result.name)
        assertEquals("", result.summary)
        assertEquals("", result.description)
        assertEquals(emptyList<String>(), result.tags)
        assertEquals("", result.license)
        assertEquals(emptyList<String>(), result.gameVersions)
        assertEquals(emptyList<String>(), result.loaders)
        assertEquals(TerracottaEnvironment.SERVER_ONLY, result.environment)
        assertEquals(TerracottaReleaseType.RELEASE, result.releaseType)
        assertEquals("", result.changelog)
        assertEquals("terracotta", result.readmeConvention)
        assertEquals("keep-a-changelog", result.changelogConvention)
    }
}
