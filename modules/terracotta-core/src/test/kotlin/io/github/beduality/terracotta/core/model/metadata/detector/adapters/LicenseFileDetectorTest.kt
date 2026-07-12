package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataContext
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LicenseFileDetectorTest {
    @Test
    fun `detects MIT from LICENSE file`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "LICENSE").writeText("MIT License\n\nCopyright (c) 2026")

        val result = LicenseFileDetector().detect(context(tempDir))

        assertEquals("MIT", result?.license)
    }

    @Test
    fun `detects Apache from LICENSE file`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "LICENSE.txt").writeText("Apache License\nVersion 2.0")

        val result = LicenseFileDetector().detect(context(tempDir))

        assertEquals("Apache-2.0", result?.license)
    }

    @Test
    fun `returns null when license file is missing`(
        @TempDir tempDir: File,
    ) {
        val result = LicenseFileDetector().detect(context(tempDir))

        assertNull(result)
    }

    @Test
    fun `detects from LICENSE without known identifier`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "LICENSE").writeText("Some custom license text")

        val result = LicenseFileDetector().detect(context(tempDir))

        assertNull(result?.license)
    }

    private fun context(tempDir: File): ProjectMetadataContext = ProjectMetadataContext(ProjectFileCache(tempDir), ProjectMetadataSource())
}
