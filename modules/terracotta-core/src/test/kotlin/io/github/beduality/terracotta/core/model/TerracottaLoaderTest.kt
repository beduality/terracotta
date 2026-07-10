package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TerracottaLoaderTest {
    @Test
    fun `fromId resolves all valid entries`() {
        TerracottaLoader.entries.forEach { entry ->
            assertEquals(entry, TerracottaLoader.fromId(entry.id))
        }
    }

    @Test
    fun `fromId is case-insensitive`() {
        TerracottaLoader.entries.forEach { entry ->
            assertEquals(entry, TerracottaLoader.fromId(entry.id.uppercase()))
            assertEquals(entry, TerracottaLoader.fromId(entry.id.replaceFirstChar { it.uppercase() }))
        }
    }

    @Test
    fun `fromId throws on invalid input`() {
        val ex = assertThrows<IllegalArgumentException> {
            TerracottaLoader.fromId("invalid_loader")
        }
        assertTrue(ex.message!!.contains("invalid_loader"))
        assertTrue(ex.message!!.contains("paper"))
        assertTrue(ex.message!!.contains("fabric"))
    }
}
