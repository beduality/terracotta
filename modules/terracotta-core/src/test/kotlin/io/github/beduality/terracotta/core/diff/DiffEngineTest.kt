package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffEngineTest {
    @Test
    fun `test diff with no remote state should create project then upload all versions`() {
        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions =
                    listOf(
                        TerracottaVersion("1.0.0", "path/to/jar", listOf("1.20")),
                        TerracottaVersion("1.1.0", "path/to/jar-2", listOf("1.20.1")),
                    ),
                tags = listOf("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, null)

        assertEquals(3, ops.size)
        assertTrue(ops[0] is Operation.CreateProject)
        assertTrue(ops[1] is Operation.UploadVersion)
        assertEquals("1.0.0", (ops[1] as Operation.UploadVersion).version.version)
        assertTrue(ops[2] is Operation.UploadVersion)
        assertEquals("1.1.0", (ops[2] as Operation.UploadVersion).version.version)
    }

    @Test
    fun `test diff with no remote state and no versions should only create project`() {
        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions = emptyList(),
                tags = listOf("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, null)

        assertEquals(1, ops.size)
        assertTrue(ops[0] is Operation.CreateProject)
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

    @Test
    fun `test diff with only name changed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Old Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "New Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.nameChanged)
        assertEquals("New Name", metaOp.newName)
    }

    @Test
    fun `test diff with only summary changed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Old Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "New Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.summaryChanged)
        assertEquals("New Summary", metaOp.newSummary)
    }

    @Test
    fun `test diff with only license changed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "Apache-2.0",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.licenseChanged)
        assertEquals("Apache-2.0", metaOp.newLicense)
    }

    @Test
    fun `test diff with license case change should not trigger update`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "mit",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with only description changed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Old description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "New description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val descOp = ops.filterIsInstance<Operation.UpdateDescription>().first()
        assertEquals("Old description", descOp.oldDescription)
        assertEquals("New description", descOp.newDescription)
    }

    @Test
    fun `test diff with tags added`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = listOf("utility"),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = listOf("utility", "new-tag"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val tagsOp = ops.filterIsInstance<Operation.UpdateTags>().first()
        assertEquals(listOf("utility"), tagsOp.oldTags)
        assertEquals(listOf("utility", "new-tag"), tagsOp.newTags)
    }

    @Test
    fun `test diff with tags removed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = listOf("utility", "old-tag"),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                tags = listOf("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val tagsOp = ops.filterIsInstance<Operation.UpdateTags>().first()
        assertEquals(listOf("utility", "old-tag"), tagsOp.oldTags)
        assertEquals(listOf("utility"), tagsOp.newTags)
    }
}
