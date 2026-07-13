package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffEngineGalleryTest {
    private val emptyProject =
        TerracottaProject(
            id = "my-plugin",
            name = "My Plugin",
            summary = "A summary",
            description = "A description",
            versions = emptyList(),
            categories = TerracottaProjectCategories(primary = TerracottaCategory("default", "Default")),
            license = "MIT",
        )

    @Test
    fun `emits upload for local gallery item not present remotely`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha")),
            )
        val remote = emptyProject

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val upload = ops.filterIsInstance<Operation.UploadGalleryItem>().first()
        assertEquals("Alpha", upload.item.title)
    }

    @Test
    fun `emits delete for remote gallery item not present locally`() {
        val local = emptyProject
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "Remote")),
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val delete = ops.filterIsInstance<Operation.DeleteGalleryItem>().first()
        assertEquals("Remote", delete.item.title)
    }

    @Test
    fun `emits update when matched gallery item metadata changes`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha", description = "New")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "Alpha", description = "Old")),
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(1, ops.size)
        val update = ops.filterIsInstance<Operation.UpdateGalleryItem>().first()
        assertEquals("Alpha", update.newItem.title)
        assertEquals("New", update.newItem.description)
        assertEquals("Old", update.oldItem.description)
    }

    @Test
    fun `matches gallery items by normalized title`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "  ALPHA  ")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "alpha")),
            )

        val ops = DiffEngine.diff(local, remote)

        assertTrue(ops.isEmpty(), "Expected no operations when titles match after normalization")
    }

    @Test
    fun `emits upload and delete when title changes`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "Beta")),
            )

        val ops = DiffEngine.diff(local, remote)

        assertEquals(2, ops.size)
        assertEquals(1, ops.filterIsInstance<Operation.UploadGalleryItem>().size)
        assertEquals(1, ops.filterIsInstance<Operation.DeleteGalleryItem>().size)
    }

    @Test
    fun `emits no gallery operations when states match`() {
        val gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha", ordering = 1))
        val local = emptyProject.copy(gallery = gallery)
        val remote = emptyProject.copy(gallery = gallery)

        val ops = DiffEngine.diff(local, remote)

        assertTrue(ops.filterIsInstance<Operation.UploadGalleryItem>().isEmpty())
        assertTrue(ops.filterIsInstance<Operation.UpdateGalleryItem>().isEmpty())
        assertTrue(ops.filterIsInstance<Operation.DeleteGalleryItem>().isEmpty())
    }
}
