package io.github.beduality.terracotta.core.model.projectfile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectFileCacheTest {
    @Test
    fun `reads file content`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "README.md").writeText("Hello")
        val cache = ProjectFileCache(tempDir)

        assertEquals("Hello", cache.read("README.md"))
    }

    @Test
    fun `returns null for missing file`(
        @TempDir tempDir: File,
    ) {
        val cache = ProjectFileCache(tempDir)

        assertNull(cache.read("missing.txt"))
    }

    @Test
    fun `returns null for directory path`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, "sub").mkdir()
        val cache = ProjectFileCache(tempDir)

        assertNull(cache.read("sub"))
    }

    @Test
    fun `caches file content so later changes are ignored`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "cached.txt")
        file.writeText("first")
        val cache = ProjectFileCache(tempDir)

        assertEquals("first", cache.read("cached.txt"))

        file.writeText("second")
        assertEquals("first", cache.read("cached.txt"))
    }
}
