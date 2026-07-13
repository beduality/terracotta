package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TerracottaVisibilityTest {
    @Test
    fun `fromId resolves all valid entries`() {
        TerracottaVisibility.entries.forEach { entry ->
            assertEquals(entry, TerracottaVisibility.fromId(entry.id))
        }
    }

    @Test
    fun `fromId is case-insensitive`() {
        TerracottaVisibility.entries.forEach { entry ->
            assertEquals(entry, TerracottaVisibility.fromId(entry.id.uppercase()))
            assertEquals(entry, TerracottaVisibility.fromId(entry.id.replaceFirstChar { it.uppercase() }))
        }
    }

    @Test
    fun `fromId throws on invalid input`() {
        val ex =
            assertThrows<IllegalArgumentException> {
                TerracottaVisibility.fromId("invalid_value")
            }
        assertTrue(ex.message!!.contains("invalid_value"))
        assertTrue(ex.message!!.contains("public"))
        assertTrue(ex.message!!.contains("unlisted"))
        assertTrue(ex.message!!.contains("archived"))
        assertTrue(ex.message!!.contains("private"))
        assertTrue(ex.message!!.contains("draft"))
    }
}
