package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReadmeMetadataDetectorTest {
    @Test
    fun `detects description from README`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # My Project

            Full description here.
            """.trimIndent(),
        )

        val result = ReadmeMetadataDetector().detect(context(tempDir))

        assertEquals(
            """
            # My Project

            Full description here.
            """.trimIndent(),
            result?.description,
        )
    }

    @Test
    fun `uses first paragraph as summary`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # My Project

            This is the summary paragraph.

            More details follow.
            """.trimIndent(),
        )

        val result = ReadmeMetadataDetector().detect(context(tempDir))

        assertEquals("This is the summary paragraph.", result?.summary)
    }

    @Test
    fun `returns null when README is missing`(
        @TempDir tempDir: File,
    ) {
        val result = ReadmeMetadataDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `ignores markdown headings when extracting summary`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText(
            """
            # My Project

            First real paragraph.
            """.trimIndent(),
        )

        val result = ReadmeMetadataDetector().detect(context(tempDir))

        assertEquals("First real paragraph.", result?.summary)
    }

    private fun context(tempDir: File): ProjectMetadataContext =
        ProjectMetadataContext(ProjectFileCache(tempDir), ProjectMetadataSource())
}
