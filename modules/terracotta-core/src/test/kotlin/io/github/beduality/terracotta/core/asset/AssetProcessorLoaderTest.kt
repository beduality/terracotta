package io.github.beduality.terracotta.core.asset

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class AssetProcessorLoaderTest {
    @Test
    fun `returns identity processor when no custom processor is registered`() {
        val processor = AssetProcessorLoader.load()

        val file = File("test.png")
        val processed = processor.process(file)

        assertEquals(file.absolutePath, processed.path)
        assertEquals("application/octet-stream", processed.contentType)
        assertEquals("png", processed.extension)
    }
}
