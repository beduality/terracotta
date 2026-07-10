package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TerracottaEnvironmentTest {
    @Test
    fun `fromId resolves all valid entries`() {
        TerracottaEnvironment.entries.forEach { entry ->
            assertEquals(entry, TerracottaEnvironment.fromId(entry.id))
        }
    }

    @Test
    fun `fromId is case-insensitive`() {
        TerracottaEnvironment.entries.forEach { entry ->
            assertEquals(entry, TerracottaEnvironment.fromId(entry.id.uppercase()))
            assertEquals(entry, TerracottaEnvironment.fromId(entry.id.replaceFirstChar { it.uppercase() }))
        }
    }

    @Test
    fun `fromId throws on invalid input`() {
        val ex =
            assertThrows<IllegalArgumentException> {
                TerracottaEnvironment.fromId("invalid_value")
            }
        assertTrue(ex.message!!.contains("invalid_value"))
        assertTrue(ex.message!!.contains("client_only"))
        assertTrue(ex.message!!.contains("server_only"))
        assertTrue(ex.message!!.contains("universal"))
    }
}
