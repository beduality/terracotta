package io.github.beduality.terracotta.core.config

import io.github.beduality.terracotta.core.model.TerracottaDonationLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TerracottaConfigLoaderLinksTest {
    @Test
    fun `loads links section from terracotta yml`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            links:
              homepage: "https://example.com"
              source: "https://github.com/example/project"
              issues: "https://github.com/example/project/issues"
              wiki: "https://github.com/example/project/wiki"
              community: "https://discord.gg/example"
              donations:
                - platform: patreon
                  url: "https://patreon.com/example"
              other:
                twitter: "https://twitter.com/example"
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertNotNull(config.links)
        val links = config.links!!
        assertEquals("https://example.com", links.homepage)
        assertEquals("https://github.com/example/project", links.source)
        assertEquals("https://github.com/example/project/issues", links.issues)
        assertEquals("https://github.com/example/project/wiki", links.wiki)
        assertEquals("https://discord.gg/example", links.community)
        assertEquals(listOf(TerracottaDonationLink("patreon", "https://patreon.com/example")), links.donations)
        assertEquals(mapOf("twitter" to "https://twitter.com/example"), links.other)
    }

    @Test
    fun `returns null links when section is absent`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText("name: Plugin\n")

        val config = TerracottaConfigLoader.load(file)

        assertNull(config.links)
    }

    @Test
    fun `ignores malformed donations`(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "terracotta.yml")
        file.writeText(
            """
            links:
              donations: "not-a-list"
            """.trimIndent(),
        )

        val config = TerracottaConfigLoader.load(file)

        assertNotNull(config.links)
        assertTrue(config.links!!.donations.isEmpty())
    }
}
