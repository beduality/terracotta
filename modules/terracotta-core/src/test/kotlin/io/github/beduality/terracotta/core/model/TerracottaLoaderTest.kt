package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TerracottaLoaderTest {
    @Test
    fun `fromId resolves all built-in loaders`() {
        TerracottaLoaderRegistry.all().forEach { loader ->
            assertEquals(loader, TerracottaLoaderRegistry.fromId(loader.id))
        }
    }

    @Test
    fun `fromId is case-insensitive`() {
        TerracottaLoaderRegistry.all().forEach { loader ->
            assertEquals(loader, TerracottaLoaderRegistry.fromId(loader.id.uppercase()))
            assertEquals(loader, TerracottaLoaderRegistry.fromId(loader.id.replaceFirstChar { it.uppercase() }))
        }
    }

    @Test
    fun `fromId throws on invalid input`() {
        val ex =
            assertThrows<IllegalArgumentException> {
                TerracottaLoaderRegistry.fromId("invalid_loader")
            }
        assertTrue(ex.message!!.contains("invalid_loader"))
        assertTrue(ex.message!!.contains("paper"))
        assertTrue(ex.message!!.contains("fabric"))
    }

    @Test
    fun `registration allows custom loaders`() {
        val customLoader = object : AbstractTerracottaLoader("custom", "Custom") {
            override fun detect(cache: io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache): Boolean = false
        }
        TerracottaLoaderRegistry.register(customLoader)

        assertEquals(customLoader, TerracottaLoaderRegistry.fromId("custom"))
    }
}
