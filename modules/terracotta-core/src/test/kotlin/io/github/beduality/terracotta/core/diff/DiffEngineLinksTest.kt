package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffEngineLinksTest {
    @Test
    fun `emits update metadata when links change`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = TerracottaProjectCategories(primary = TerracottaCategory("default", "Default")),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = TerracottaProjectCategories(primary = TerracottaCategory("default", "Default")),
                license = "MIT",
                links = TerracottaProjectLinks(source = "https://github.com/example/project"),
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.linksChanged)
        assertFalse(metaOp.nameChanged)
        assertEquals("https://github.com/example/project", metaOp.newLinks.source)
    }

    @Test
    fun `no operation when links are identical`() {
        val links = TerracottaProjectLinks(source = "https://github.com/example/project")
        val project =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = TerracottaProjectCategories(primary = TerracottaCategory("default", "Default")),
                license = "MIT",
                links = links,
            )

        val ops = DiffEngine.diff(project, project)

        assertTrue(ops.isEmpty())
    }
}
