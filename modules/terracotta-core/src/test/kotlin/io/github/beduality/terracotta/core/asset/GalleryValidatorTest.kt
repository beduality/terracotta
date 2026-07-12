package io.github.beduality.terracotta.core.asset

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class GalleryValidatorTest {
    private val extensions = setOf("png", "jpg", "webp")
    private val maxSize = 1024L

    @Test
    fun `throws when file does not exist`(
        @TempDir tempDir: File,
    ) {
        val missing = File(tempDir, "missing.png")

        val exception =
            assertThrows(IOException::class.java) {
                GalleryValidator.validate(missing.absolutePath, extensions, maxSize)
            }

        assertTrue(exception.message?.contains("not found") == true)
    }

    @Test
    fun `throws when extension is unsupported`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "image.gif")
        file.writeBytes(ByteArray(10))

        val exception =
            assertThrows(IOException::class.java) {
                GalleryValidator.validate(file.absolutePath, extensions, maxSize)
            }

        assertTrue(exception.message?.contains("Unsupported") == true)
    }

    @Test
    fun `throws when file exceeds max size`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "image.png")
        file.writeBytes(ByteArray(maxSize.toInt() + 1))

        val exception =
            assertThrows(IOException::class.java) {
                GalleryValidator.validate(file.absolutePath, extensions, maxSize)
            }

        assertTrue(exception.message?.contains("size") == true)
    }

    @Test
    fun `passes for valid file`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "image.png")
        file.writeBytes(ByteArray(10))

        GalleryValidator.validate(file.absolutePath, extensions, maxSize)
    }
}
