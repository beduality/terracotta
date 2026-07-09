package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffEngineTest {
    @Test
    fun `test diff with no remote state should create everything`() {
        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions =
                    listOf(
                        TerracottaVersion("1.0.0", "path/to/jar", listOf("1.20")),
                    ),
                tags = listOf("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, null)

        assertEquals(2, ops.size)
        assertTrue(ops[0] is Operation.CreateProject)
        assertTrue(ops[1] is Operation.UploadVersion)
    }

    @Test
    fun `test diff with identical states should return no operations`() {
        val project =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions =
                    listOf(
                        TerracottaVersion("1.0.0", "path/to/jar", listOf("1.20")),
                    ),
                tags = listOf("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(project, project)

        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with modified metadata, description, tags, and new version`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "My Old Plugin",
                summary = "An old summary",
                description = "Old description",
                versions =
                    listOf(
                        TerracottaVersion("1.0.0", "path/to/jar", listOf("1.20")),
                    ),
                tags = listOf("utility"),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "My New Plugin",
                summary = "A new summary",
                description = "New description",
                versions =
                    listOf(
                        TerracottaVersion("1.0.0", "path/to/jar", listOf("1.20")),
                        TerracottaVersion("1.1.0", "path/to/new-jar", listOf("1.20.1")),
                    ),
                tags = listOf("utility", "new-tag"),
                license = "Apache-2.0",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(4, ops.size)

        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.nameChanged)
        assertTrue(metaOp.summaryChanged)
        assertTrue(metaOp.licenseChanged)
        assertEquals("My New Plugin", metaOp.newName)

        val descOp = ops.filterIsInstance<Operation.UpdateDescription>().first()
        assertEquals("Old description", descOp.oldDescription)
        assertEquals("New description", descOp.newDescription)

        val tagsOp = ops.filterIsInstance<Operation.UpdateTags>().first()
        assertEquals(listOf("utility", "new-tag"), tagsOp.newTags)

        val versionOp = ops.filterIsInstance<Operation.UploadVersion>().first()
        assertEquals("1.1.0", versionOp.version.version)
    }
}
