package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaVisibility
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
                categories = tags("utility"),
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
                categories = tags("utility"),
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
                categories = tags("utility"),
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
                categories = tags("utility"),
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
                categories = tags("utility", "new-tag"),
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

        val categoriesOp = ops.filterIsInstance<Operation.UpdateCategories>().first()
        assertEquals(
            listOf("utility", "new-tag"),
            categoriesOp.newCategories.let {
                listOf(it.primary.id) +
                    it.additional.map {
                            c ->
                        c.id
                    }
            },
        )

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
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "New Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
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
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "New Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
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
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
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
                categories = tags(),
                license = "mit",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with only licenseUrl changed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/LICENSE",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.licenseUrlChanged)
        assertEquals("https://example.com/LICENSE", metaOp.newLicenseUrl)
        assertFalse(metaOp.nameChanged)
        assertFalse(metaOp.summaryChanged)
        assertFalse(metaOp.licenseChanged)
    }

    @Test
    fun `test diff with only licenseUrl removed should trigger update`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/old",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.licenseUrlChanged)
        assertNull(metaOp.newLicenseUrl)
    }

    @Test
    fun `test diff with same licenseUrl should not trigger update`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/LICENSE",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/LICENSE",
            )

        val ops = DiffEngine.diff(local, remote)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with licenseUrl ignored when provider does not support licenseUrl`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/LICENSE",
            )

        val ops = DiffEngine.diff(local, remote, supportsLicenseUrl = false)

        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with licenseUrl and metadata change only sets licenseUrl when supported`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Old Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "New Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                licenseUrl = "https://example.com/LICENSE",
            )

        val ops = DiffEngine.diff(local, remote, supportsLicenseUrl = false)

        assertEquals(1, ops.size)
        val metaOp = ops.filterIsInstance<Operation.UpdateMetadata>().first()
        assertTrue(metaOp.nameChanged)
        assertFalse(metaOp.licenseUrlChanged)
        assertNull(metaOp.newLicenseUrl)
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
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "New description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val descOp = ops.filterIsInstance<Operation.UpdateDescription>().first()
        assertEquals("Old description", descOp.oldDescription)
        assertEquals("New description", descOp.newDescription)
    }

    @Test
    fun `test diff with categories added`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags("utility"),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags("utility", "new-tag"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val categoriesOp = ops.filterIsInstance<Operation.UpdateCategories>().first()
        assertEquals(listOf("utility"), categoriesOp.oldCategories.let { listOf(it.primary.id) + it.additional.map { c -> c.id } })
        assertEquals(
            listOf("utility", "new-tag"),
            categoriesOp.newCategories.let {
                listOf(it.primary.id) +
                    it.additional.map {
                            c ->
                        c.id
                    }
            },
        )
    }

    @Test
    fun `test diff with categories removed`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags("utility", "old-tag"),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags("utility"),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val categoriesOp = ops.filterIsInstance<Operation.UpdateCategories>().first()
        assertEquals(
            listOf("utility", "old-tag"),
            categoriesOp.oldCategories.let {
                listOf(it.primary.id) +
                    it.additional.map {
                            c ->
                        c.id
                    }
            },
        )
        assertEquals(listOf("utility"), categoriesOp.newCategories.let { listOf(it.primary.id) + it.additional.map { c -> c.id } })
    }

    @Test
    fun `test diff with icon upload when remote has no icon`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                icon = "docs/assets/icon.png",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val iconOp = ops.filterIsInstance<Operation.UploadIcon>().first()
        assertEquals("docs/assets/icon.png", iconOp.iconPath)
    }

    @Test
    fun `test diff with icon update when remote icon differs`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                icon = "https://hangar.papermc.io/avatars/old.png",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                icon = "docs/assets/icon.png",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val iconOp = ops.filterIsInstance<Operation.UpdateIcon>().first()
        assertEquals("https://hangar.papermc.io/avatars/old.png", iconOp.oldIconUrl)
        assertEquals("docs/assets/icon.png", iconOp.iconPath)
    }

    @Test
    fun `test diff with icon deletion when local removes icon`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                icon = "https://hangar.papermc.io/avatars/old.png",
            )

        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
            )

        val ops = DiffEngine.diff(local, remote)
        assertEquals(1, ops.size)
        val iconOp = ops.filterIsInstance<Operation.DeleteIcon>().first()
        assertEquals("https://hangar.papermc.io/avatars/old.png", iconOp.iconUrl)
    }

    @Test
    fun `test diff with identical icons should not trigger update`() {
        val project =
            TerracottaProject(
                id = "my-plugin",
                name = "Name",
                summary = "Summary",
                description = "Description",
                versions = emptyList(),
                categories = tags(),
                license = "MIT",
                icon = "docs/assets/icon.png",
            )

        val ops = DiffEngine.diff(project, project)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun `test diff with visibility change should emit UpdateVisibility`() {
        val remote =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions = emptyList(),
                categories = tags("utility"),
                license = "MIT",
                visibility = TerracottaVisibility.PUBLIC,
            )
        val local =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions = emptyList(),
                categories = tags("utility"),
                license = "MIT",
                visibility = TerracottaVisibility.UNLISTED,
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val visibilityOp = ops.filterIsInstance<Operation.UpdateVisibility>().first()
        assertEquals(TerracottaVisibility.PUBLIC, visibilityOp.oldVisibility)
        assertEquals(TerracottaVisibility.UNLISTED, visibilityOp.newVisibility)
    }

    @Test
    fun `test diff with identical visibility should not trigger update`() {
        val project =
            TerracottaProject(
                id = "my-plugin",
                name = "My Plugin",
                summary = "A summary",
                description = "Some description",
                versions = emptyList(),
                categories = tags("utility"),
                license = "MIT",
                visibility = TerracottaVisibility.ARCHIVED,
            )

        val ops = DiffEngine.diff(project, project)

        assertTrue(ops.isEmpty())
    }

    private fun tags(vararg ids: String): TerracottaProjectCategories {
        val primary = ids.firstOrNull() ?: "default"
        return TerracottaProjectCategories(
            primary = TerracottaCategory(primary, primary),
            additional = ids.drop(1).map { TerracottaCategory(it, it) },
        )
    }
}
