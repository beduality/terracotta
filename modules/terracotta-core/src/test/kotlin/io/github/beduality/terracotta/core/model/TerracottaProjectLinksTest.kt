package io.github.beduality.terracotta.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerracottaProjectLinksTest {
    @Test
    fun `defaults to empty links`() {
        val links = TerracottaProjectLinks()

        assertNull(links.homepage)
        assertNull(links.source)
        assertNull(links.issues)
        assertNull(links.wiki)
        assertNull(links.community)
        assertTrue(links.donations.isEmpty())
        assertTrue(links.other.isEmpty())
        assertTrue(links.isEmpty())
    }

    @Test
    fun `holds all link fields`() {
        val links =
            TerracottaProjectLinks(
                homepage = "https://example.com",
                source = "https://github.com/example/project",
                issues = "https://github.com/example/project/issues",
                wiki = "https://github.com/example/project/wiki",
                community = "https://discord.gg/example",
                donations = listOf(TerracottaDonationLink("patreon", "https://patreon.com/example")),
                other = mapOf("twitter" to "https://twitter.com/example"),
            )

        assertEquals("https://example.com", links.homepage)
        assertEquals("https://github.com/example/project", links.source)
        assertEquals("https://github.com/example/project/issues", links.issues)
        assertEquals("https://github.com/example/project/wiki", links.wiki)
        assertEquals("https://discord.gg/example", links.community)
        assertEquals(1, links.donations.size)
        assertEquals("patreon", links.donations.first().platform)
        assertEquals("https://patreon.com/example", links.donations.first().url)
        assertEquals("https://twitter.com/example", links.other["twitter"])
        assertTrue(!links.isEmpty())
    }
}
