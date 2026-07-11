package io.github.beduality.terracotta.core.model.projectfile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReadmeFileTest {
    @Test
    fun `uses full content as description`() {
        val content =
            """
            # My Project

            Full description here.
            """.trimIndent()

        val file = ReadmeFile(content)

        assertEquals(content, file.description)
    }

    @Test
    fun `uses first paragraph as summary`() {
        val content =
            """
            # My Project

            This is the summary paragraph.

            More details follow.
            """.trimIndent()

        val file = ReadmeFile(content)

        assertEquals("This is the summary paragraph.", file.summary)
    }

    @Test
    fun `ignores markdown headings when extracting summary`() {
        val content =
            """
            # My Project

            First real paragraph.
            """.trimIndent()

        val file = ReadmeFile(content)

        assertEquals("First real paragraph.", file.summary)
    }

    @Test
    fun `returns null when content is missing`() {
        val file = ReadmeFile(null)

        assertNull(file.description)
        assertNull(file.summary)
    }

    @Test
    fun `returns null when content is empty`() {
        val file = ReadmeFile("   ")

        assertNull(file.description)
        assertNull(file.summary)
    }
}
