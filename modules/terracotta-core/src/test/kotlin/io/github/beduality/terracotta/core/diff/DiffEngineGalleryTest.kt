package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.state.GalleryItemIdentity
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

    @Test
    fun `matches gallery item by stable key across title changes`() {
        val localKey = "alpha-key"
        val remoteUrl = "https://cdn/remote.png"
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", key = localKey, title = "Alpha New")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = remoteUrl, title = "Alpha Old")),
            )
        val persisted = mapOf(localKey to GalleryItemIdentity(localKey = localKey, remoteUrl = remoteUrl))

        val ops = DiffEngine.diff(local, remote, persisted)

        assertEquals(1, ops.size)
        val update = ops.filterIsInstance<Operation.UpdateGalleryItem>().first()
        assertEquals("Alpha Old", update.oldItem.title)
        assertEquals("Alpha New", update.newItem.title)
        assertEquals(remoteUrl, update.oldItem.imagePath)
    }

    @Test
    fun `falls back to title matching when no persisted identity exists`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "Alpha")),
            )

        val ops = DiffEngine.diff(local, remote, emptyMap())

        assertTrue(ops.isEmpty(), "Expected no operations when title matches without persisted identity")
    }

    @Test
    fun `uploads and deletes when identity match is absent and title changes`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", key = "alpha-key", title = "Alpha")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "https://cdn/remote.png", title = "Beta")),
            )
        val persisted = mapOf("alpha-key" to GalleryItemIdentity(localKey = "alpha-key", remoteUrl = "https://cdn/other.png"))

        val ops = DiffEngine.diff(local, remote, persisted)

        assertEquals(2, ops.size)
        assertEquals(1, ops.filterIsInstance<Operation.UploadGalleryItem>().size)
        assertEquals(1, ops.filterIsInstance<Operation.DeleteGalleryItem>().size)
    }

    @Test
    fun `matches gallery item by stable key when remote title changed beyond recognition`() {
        val localKey = "alpha-key"
        val remoteUrl = "https://cdn/remote.png"
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", key = localKey, title = "Alpha")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = remoteUrl, title = "Completely different title")),
            )
        val persisted = mapOf(localKey to GalleryItemIdentity(localKey = localKey, remoteUrl = remoteUrl))

        val ops = DiffEngine.diff(local, remote, persisted)

        assertEquals(1, ops.size)
        val update = ops.filterIsInstance<Operation.UpdateGalleryItem>().first()
        assertEquals("Completely different title", update.oldItem.title)
        assertEquals("Alpha", update.newItem.title)
    }

    @Test
    fun `matches gallery item by image path when no explicit key is provided`() {
        val remoteUrl = "https://cdn/remote.png"
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", title = "Alpha New")),
            )
        val remote =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = remoteUrl, title = "Alpha Old")),
            )
        val persisted = mapOf("a.png" to GalleryItemIdentity(localKey = "a.png", remoteUrl = remoteUrl))

        val ops = DiffEngine.diff(local, remote, persisted)

        assertEquals(1, ops.size)
        val update = ops.filterIsInstance<Operation.UpdateGalleryItem>().first()
        assertEquals("Alpha Old", update.oldItem.title)
        assertEquals("Alpha New", update.newItem.title)
    }

    @Test
    fun `uploads new gallery item when identity exists but remote url does not`() {
        val local =
            emptyProject.copy(
                gallery = listOf(TerracottaGalleryItem(imagePath = "a.png", key = "alpha-key", title = "Alpha")),
            )
        val remote = emptyProject
        val persisted = mapOf("alpha-key" to GalleryItemIdentity(localKey = "alpha-key", remoteUrl = "https://cdn/missing.png"))

        val ops = DiffEngine.diff(local, remote, persisted)

        assertEquals(1, ops.size)
        val upload = ops.filterIsInstance<Operation.UploadGalleryItem>().first()
        assertEquals("Alpha", upload.item.title)
    }
}
