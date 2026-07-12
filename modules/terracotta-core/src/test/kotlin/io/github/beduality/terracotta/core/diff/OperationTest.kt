package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OperationTest {
    @Test
    fun `CreateProject description contains plus prefix and project name`() {
        val project =
            TerracottaProject(
                id = "test-id",
                name = "My Plugin",
                summary = "A summary",
                description = "Description",
                versions = emptyList(),
                tags = emptyList(),
                license = "MIT",
            )
        val op = Operation.CreateProject(project)

        assertTrue(op.description.contains("+"))
        assertTrue(op.description.contains("My Plugin"))
    }

    @Test
    fun `UploadVersion description contains plus prefix and version number`() {
        val version = TerracottaVersion("2.0.0", "path/to/jar", listOf("1.20"))
        val op = Operation.UploadVersion(version)

        assertTrue(op.description.contains("+"))
        assertTrue(op.description.contains("2.0.0"))
    }

    @Test
    fun `UpdateDescription description contains tilde prefix and Update description`() {
        val op = Operation.UpdateDescription("old", "new")

        assertTrue(op.description.contains("~"))
        assertTrue(op.description.contains("Update description"))
    }

    @Test
    fun `UpdateTags description contains tilde prefix and both old and new tag values`() {
        val op =
            Operation.UpdateTags(
                oldTags = listOf("utility", "paper"),
                newTags = listOf("utility", "fabric"),
            )

        assertTrue(op.description.contains("~"))
        assertTrue(op.description.contains("utility"))
        assertTrue(op.description.contains("paper"))
        assertTrue(op.description.contains("fabric"))
    }

    @Test
    fun `UpdateMetadata description with only nameChanged contains name but not summary or license`() {
        val op =
            Operation.UpdateMetadata(
                nameChanged = true,
                summaryChanged = false,
                licenseChanged = false,
                licenseUrlChanged = false,
                linksChanged = false,
                newName = "New Name",
                newSummary = "",
                newLicense = "",
                newLicenseUrl = null,
                newLinks = TerracottaProjectLinks(),
            )

        assertTrue(op.description.contains("name"))
        assertFalse(op.description.contains("summary"))
        assertFalse(op.description.contains("license"))
    }

    @Test
    fun `UpdateMetadata description with all three changed contains name, summary, and license`() {
        val op =
            Operation.UpdateMetadata(
                nameChanged = true,
                summaryChanged = true,
                licenseChanged = true,
                licenseUrlChanged = false,
                linksChanged = false,
                newName = "New Name",
                newSummary = "New Summary",
                newLicense = "Apache-2.0",
                newLicenseUrl = null,
                newLinks = TerracottaProjectLinks(),
            )

        assertTrue(op.description.contains("name"))
        assertTrue(op.description.contains("summary"))
        assertTrue(op.description.contains("license"))
    }

    @Test
    fun `UpdateMetadata description with only summaryChanged contains summary but not name or license`() {
        val op =
            Operation.UpdateMetadata(
                nameChanged = false,
                summaryChanged = true,
                licenseChanged = false,
                licenseUrlChanged = false,
                linksChanged = false,
                newName = "",
                newSummary = "New Summary",
                newLicense = "",
                newLicenseUrl = null,
                newLinks = TerracottaProjectLinks(),
            )

        assertTrue(op.description.contains("summary"))
        assertFalse(op.description.contains("name"))
        assertFalse(op.description.contains("license"))
    }
}
