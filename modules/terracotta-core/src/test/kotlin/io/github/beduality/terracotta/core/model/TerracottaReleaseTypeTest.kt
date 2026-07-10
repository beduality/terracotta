package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TerracottaReleaseTypeTest {
    @Test
    fun `fromId resolves all valid entries`() {
        TerracottaReleaseType.entries.forEach { entry ->
            assertEquals(entry, TerracottaReleaseType.fromId(entry.id))
        }
    }

    @Test
    fun `fromId is case-insensitive`() {
        TerracottaReleaseType.entries.forEach { entry ->
            assertEquals(entry, TerracottaReleaseType.fromId(entry.id.uppercase()))
            assertEquals(entry, TerracottaReleaseType.fromId(entry.id.replaceFirstChar { it.uppercase() }))
        }
    }

    @Test
    fun `fromId throws on invalid input`() {
        val ex = assertThrows<IllegalArgumentException> {
            TerracottaReleaseType.fromId("invalid_type")
        }
        assertTrue(ex.message!!.contains("invalid_type"))
        assertTrue(ex.message!!.contains("release"))
        assertTrue(ex.message!!.contains("beta"))
        assertTrue(ex.message!!.contains("alpha"))
    }
}
