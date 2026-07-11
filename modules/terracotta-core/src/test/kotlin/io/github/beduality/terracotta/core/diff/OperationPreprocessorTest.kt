package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [OperationPreprocessor].
 *
 * Tests specific examples and edge cases for each normalization rule.
 *
 * **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 3.1, 3.3, 5.1, 5.2**
 */
class OperationPreprocessorTest {
    @Test
    fun `empty changelog on UploadVersion gets default`() {
        val version = version(changelog = "")
        val operation = Operation.UploadVersion(version)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.UploadVersion).version

        assertEquals("Uploaded via Terracotta.", processed.changelog)
    }

    @Test
    fun `non-empty changelog on UploadVersion is preserved`() {
        val version = version(changelog = "Fixed critical bug in teleportation logic.")
        val operation = Operation.UploadVersion(version)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.UploadVersion).version

        assertEquals("Fixed critical bug in teleportation logic.", processed.changelog)
    }

    @Test
    fun `display name is set to Version {version} on UploadVersion`() {
        val version = version(versionStr = "2.1.0")
        val operation = Operation.UploadVersion(version)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.UploadVersion).version

        assertEquals("Version 2.1.0", processed.displayName)
    }

    @Test
    fun `display name overwrites any existing displayName value`() {
        val version = version(versionStr = "1.5.0", displayName = "Custom Name")
        val operation = Operation.UploadVersion(version)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.UploadVersion).version

        assertEquals("Version 1.5.0", processed.displayName)
    }

    @Test
    fun `CreateProject with multiple versions normalizes all versions`() {
        val versions =
            listOf(
                version(versionStr = "1.0.0", changelog = ""),
                version(versionStr = "1.1.0", changelog = "Bug fixes"),
                version(versionStr = "1.2.0", changelog = ""),
            )
        val project = project(versions)
        val operation = Operation.CreateProject(project)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.CreateProject).project

        assertEquals(3, processed.versions.size)

        assertEquals("Uploaded via Terracotta.", processed.versions[0].changelog)
        assertEquals("Version 1.0.0", processed.versions[0].displayName)

        assertEquals("Bug fixes", processed.versions[1].changelog)
        assertEquals("Version 1.1.0", processed.versions[1].displayName)

        assertEquals("Uploaded via Terracotta.", processed.versions[2].changelog)
        assertEquals("Version 1.2.0", processed.versions[2].displayName)
    }

    @Test
    fun `UpdateMetadata passes through unchanged`() {
        val operation =
            Operation.UpdateMetadata(
                nameChanged = true,
                summaryChanged = true,
                licenseChanged = false,
                newName = "Updated Plugin",
                newSummary = "New summary",
                newLicense = "",
            )

        val result = OperationPreprocessor.process(listOf(operation))

        assertEquals(operation, result[0])
    }

    @Test
    fun `UpdateDescription passes through unchanged`() {
        val operation = Operation.UpdateDescription("old description", "new description")

        val result = OperationPreprocessor.process(listOf(operation))

        assertEquals(operation, result[0])
    }

    @Test
    fun `UpdateTags passes through unchanged`() {
        val operation = Operation.UpdateTags(listOf("adventure", "pvp"), listOf("utility", "economy"))

        val result = OperationPreprocessor.process(listOf(operation))

        assertEquals(operation, result[0])
    }

    @Test
    fun `empty input list returns empty output list`() {
        val result = OperationPreprocessor.process(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `version fields other than changelog and displayName are preserved`() {
        val version =
            TerracottaVersion(
                version = "3.0.0",
                artifactPath = "/custom/path/artifact.jar",
                gameVersions = listOf("1.20.4", "1.20.6"),
                loaders = listOf("fabric", "quilt"),
                environment = TerracottaEnvironment.UNIVERSAL,
                releaseType = TerracottaReleaseType.BETA,
                changelog = "Some notes",
            )
        val operation = Operation.UploadVersion(version)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.UploadVersion).version

        assertEquals("3.0.0", processed.version)
        assertEquals("/custom/path/artifact.jar", processed.artifactPath)
        assertEquals(listOf("1.20.4", "1.20.6"), processed.gameVersions)
        assertEquals(listOf("fabric", "quilt"), processed.loaders)
        assertEquals(TerracottaEnvironment.UNIVERSAL, processed.environment)
        assertEquals(TerracottaReleaseType.BETA, processed.releaseType)
        assertEquals("Some notes", processed.changelog)
        assertEquals("Version 3.0.0", processed.displayName)
    }

    @Test
    fun `CreateProject metadata is not modified`() {
        val project =
            TerracottaProject(
                id = "my-mod",
                name = "My Mod",
                summary = "Cool mod",
                description = "A really cool mod",
                versions = listOf(version(versionStr = "1.0.0", changelog = "")),
                tags = listOf("magic", "adventure"),
                license = "Apache-2.0",
            )
        val operation = Operation.CreateProject(project)

        val result = OperationPreprocessor.process(listOf(operation))
        val processed = (result[0] as Operation.CreateProject).project

        assertEquals("my-mod", processed.id)
        assertEquals("My Mod", processed.name)
        assertEquals("Cool mod", processed.summary)
        assertEquals("A really cool mod", processed.description)
        assertEquals(listOf("magic", "adventure"), processed.tags)
        assertEquals("Apache-2.0", processed.license)
    }

    private fun version(
        versionStr: String = "1.0.0",
        changelog: String = "",
        displayName: String = "",
    ): TerracottaVersion =
        TerracottaVersion(
            version = versionStr,
            artifactPath = "/path/to/$versionStr.jar",
            gameVersions = listOf("1.20.4"),
            loaders = listOf("paper"),
            environment = TerracottaEnvironment.SERVER_ONLY,
            releaseType = TerracottaReleaseType.RELEASE,
            changelog = changelog,
            displayName = displayName,
        )

    private fun project(versions: List<TerracottaVersion>): TerracottaProject =
        TerracottaProject(
            id = "test-plugin",
            name = "Test Plugin",
            summary = "A test plugin",
            description = "Description for testing",
            versions = versions,
            tags = listOf("utility"),
            license = "MIT",
        )
}
