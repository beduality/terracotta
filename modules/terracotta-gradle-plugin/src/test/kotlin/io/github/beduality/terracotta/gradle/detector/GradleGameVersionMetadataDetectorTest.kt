package io.github.beduality.terracotta.gradle.detector

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleGameVersionMetadataDetectorTest {
    @Test
    fun `detects game version from gradle properties`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("minecraft_version=1.20.1")

        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `detects game version from build gradle kts paper-api coordinate`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "build.gradle.kts").writeText(
            """
            dependencies {
                paper-api("1.20.1-R0.1-SNAPSHOT")
            }
            """.trimIndent(),
        )

        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `detects game version from libs versions toml`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle").mkdirs()
        File(tempDir, "gradle/libs.versions.toml").writeText(
            """
            [versions]
            minecraft = "1.20.1"
            """.trimIndent(),
        )

        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1"), result?.gameVersions)
    }

    @Test
    fun `returns null when no game version can be detected`(
        @TempDir tempDir: File,
    ) {
        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `combines game versions from multiple sources`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("minecraft_version=1.20.1")
        File(tempDir, "build.gradle.kts").writeText(
            """
            dependencies {
                paper-api("1.20-R0.1-SNAPSHOT")
            }
            """.trimIndent(),
        )

        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.20.1", "1.20"), result?.gameVersions)
    }

    @Test
    fun `detects pre-release game version from build gradle kts`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "build.gradle.kts").writeText(
            """
            dependencies {
                paper-api("1.21.5-pre1-R0.1-SNAPSHOT")
            }
            """.trimIndent(),
        )

        val result = GradleGameVersionMetadataDetector().detect(context(tempDir))

        assertEquals(listOf("1.21.5-pre1"), result?.gameVersions)
    }

    private fun context(tempDir: File): ProjectMetadataContext =
        ProjectMetadataContext(
            ProjectFileCache(tempDir),
            ProjectMetadataSource(),
        )
}
