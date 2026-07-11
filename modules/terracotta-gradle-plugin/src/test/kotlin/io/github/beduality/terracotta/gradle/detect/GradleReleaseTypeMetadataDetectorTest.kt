package io.github.beduality.terracotta.gradle.detect

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleReleaseTypeMetadataDetectorTest {
    @Test
    fun `detects beta from gradle properties`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version=1.0.0-beta.1")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaReleaseType.BETA, result?.releaseType)
    }

    @Test
    fun `detects release from gradle properties`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version=2.0.0")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaReleaseType.RELEASE, result?.releaseType)
    }

    @Test
    fun `detects alpha from gradle properties`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version=0.1.0-alpha.1")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaReleaseType.ALPHA, result?.releaseType)
    }

    @Test
    fun `returns null when version is unspecified`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version=unspecified")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `returns null when gradle properties is missing`(
        @TempDir tempDir: File,
    ) {
        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `returns null when version is unparseable`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version=not-a-version")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `ignores whitespace around version value`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText("version = 1.0.0-SNAPSHOT")

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaReleaseType.BETA, result?.releaseType)
    }

    @Test
    fun `uses first version property when multiple are present`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "gradle.properties").writeText(
            """
            version=1.0.0-beta.1
            version=2.0.0
            """.trimIndent(),
        )

        val result = GradleReleaseTypeMetadataDetector().detect(context(tempDir))

        assertEquals(TerracottaReleaseType.BETA, result?.releaseType)
    }

    private fun context(tempDir: File): ProjectMetadataContext =
        ProjectMetadataContext(
            ProjectFileCache(tempDir),
            ProjectMetadataSource(),
        )
}
