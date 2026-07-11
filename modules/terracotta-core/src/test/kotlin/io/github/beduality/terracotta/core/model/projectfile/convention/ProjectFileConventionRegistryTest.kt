package io.github.beduality.terracotta.core.model.projectfile.convention

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectFileConventionRegistryTest {
    @Test
    fun `resolves default readme convention`() {
        ProjectFileConventionRegistry.load()

        val convention = ProjectFileConventionRegistry.resolve("terracotta")

        assertSame(TerracottaReadmeConvention, convention)
    }

    @Test
    fun `resolves default changelog convention`() {
        ProjectFileConventionRegistry.load()

        val convention = ProjectFileConventionRegistry.resolve("keep-a-changelog")

        assertSame(KeepAChangelogConvention, convention)
    }

    @Test
    fun `resolve is case insensitive`() {
        ProjectFileConventionRegistry.load()

        assertSame(TerracottaReadmeConvention, ProjectFileConventionRegistry.resolve("TERRACOTTA"))
        assertSame(KeepAChangelogConvention, ProjectFileConventionRegistry.resolve("Keep-A-Changelog"))
    }

    @Test
    fun `resolve throws for unknown convention`() {
        ProjectFileConventionRegistry.load()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            ProjectFileConventionRegistry.resolve("unknown")
        }

        assertTrue("unknown" in exception.message!!)
    }

    @Test
    fun `register allows custom conventions`() {
        val custom = object : ReadmeConvention {
            override fun resolve(id: String): ReadmeConvention? = if (id == "custom") this else null
            override fun extractDescription(content: String): String? = content
            override fun extractSummary(content: String): String? = content
        }
        ProjectFileConventionRegistry.register(custom)

        val resolved = ProjectFileConventionRegistry.resolve("custom")

        assertSame(custom, resolved)
    }
}
