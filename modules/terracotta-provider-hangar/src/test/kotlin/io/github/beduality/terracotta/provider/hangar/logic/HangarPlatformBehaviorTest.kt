package io.github.beduality.terracotta.provider.hangar.logic

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HangarPlatformBehaviorTest {
    @Test
    fun `Hangar platform behavior is stateful`() {
        assertTrue(HangarProviderLogic.platformBehavior.isStateful)
    }

    @Test
    fun `Hangar platform behavior keeps supported operations`() {
        val version =
            TerracottaVersion(
                version = "1.0.0",
                artifactPath = "",
                gameVersions = emptyList(),
                releaseType = TerracottaReleaseType.RELEASE,
            )
        val operations =
            listOf(
                Operation.UpdateMetadata(
                    nameChanged = true,
                    summaryChanged = false,
                    licenseChanged = false,
                    licenseUrlChanged = false,
                    linksChanged = false,
                    newName = "New",
                    newSummary = "",
                    newLicense = "",
                    newLicenseUrl = null,
                    newLinks = TerracottaProjectLinks(),
                ),
                Operation.UpdateDescription("old", "new"),
                Operation.UpdateCategories(
                    TerracottaProjectCategories(primary = TerracottaCategory("old", "Old")),
                    TerracottaProjectCategories(primary = TerracottaCategory("tag", "Tag")),
                ),
                Operation.UploadVersion(version),
            )

        assertEquals(operations, HangarProviderLogic.platformBehavior.filterOperations(operations))
    }

    @Test
    fun `Hangar platform behavior filters unsupported operations`() {
        val project =
            TerracottaProject(
                id = "p",
                name = "P",
                summary = "",
                description = "",
                versions = emptyList(),
                categories = TerracottaProjectCategories(primary = TerracottaCategory("default", "Default")),
                license = "",
            )
        val item = TerracottaGalleryItem(imagePath = "image.png", title = "")
        val operations =
            listOf(
                Operation.CreateProject(project),
                Operation.UploadGalleryItem(item),
                Operation.UpdateGalleryItem(item, item),
                Operation.DeleteGalleryItem(item),
                Operation.UploadIcon("icon.png"),
                Operation.UpdateIcon(oldIconUrl = null, iconPath = "icon.png"),
                Operation.DeleteIcon("https://cdn/old.png"),
            )

        assertEquals(emptyList<Operation>(), HangarProviderLogic.platformBehavior.filterOperations(operations))
    }
}
