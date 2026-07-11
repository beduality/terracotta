package io.github.beduality.terracotta.core.model.projectfile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChangelogFileTest {
    @Test
    fun `extracts version section`() {
        val changelog =
            """
            # Changelog

            ## [Unreleased]

            ### Added
            - something

            ## [1.2.0]

            ### Fixed
            - a bug

            ## [1.1.0]

            ### Changed
            - old change
            """.trimIndent()

        val result = ChangelogFile(changelog).extractVersionSection("1.2.0")

        assertEquals(
            """
            ### Fixed
            - a bug
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `extracts last version section when no following header`() {
        val changelog =
            """
            ## [1.2.0]

            ### Added
            - feature
            """.trimIndent()

        val result = ChangelogFile(changelog).extractVersionSection("1.2.0")

        assertEquals(
            """
            ### Added
            - feature
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `returns null when version not found`() {
        val changelog =
            """
            ## [1.0.0]

            - change
            """.trimIndent()

        val result = ChangelogFile(changelog).extractVersionSection("2.0.0")

        assertNull(result)
    }

    @Test
    fun `escapes version regex special characters`() {
        val changelog =
            """
            ## [1.0.0-beta.1]

            - beta change
            """.trimIndent()

        val result = ChangelogFile(changelog).extractVersionSection("1.0.0-beta.1")

        assertEquals("- beta change", result)
    }

    @Test
    fun `returns null when changelog content is missing`() {
        val result = ChangelogFile(null).extractVersionSection("1.0.0")

        assertNull(result)
    }
}
