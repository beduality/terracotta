package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Property-based tests for [OperationPreprocessor].
 *
 * Tests universal invariants across generated inputs without requiring
 * external PBT libraries — uses JUnit @TestFactory with DynamicTest.
 *
 * **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 3.1, 3.3, 5.1, 5.2**
 */
class OperationPreprocessorPropertyTest {
    private val versionStrings =
        listOf(
            "1.0.0",
            "0.1.0",
            "2.5.3",
            "1.0.0-SNAPSHOT",
            "3.0.0-beta.1",
            "1.0.0-rc.2",
            "0.0.1",
            "10.20.30",
            "1",
            "v2.0",
        )

    private val changelogs =
        listOf(
            "",
            "Fixed a bug",
            "Added new feature\n\n- Item 1\n- Item 2",
            "   ",
            "Version release notes with special chars: @#\$%",
        )

    private val sampleVersions: List<TerracottaVersion> =
        versionStrings.flatMap { version ->
            changelogs.map { changelog ->
                TerracottaVersion(
                    version = version,
                    artifactPath = "/path/to/$version.jar",
                    gameVersions = listOf("1.20.4"),
                    loaders = listOf("paper"),
                    environment = TerracottaEnvironment.SERVER_ONLY,
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = changelog,
                )
            }
        }

    /**
     * Property 1: Changelog Normalization
     *
     * For any TerracottaVersion processed through OperationPreprocessor:
     * - Empty changelog → "Uploaded via Terracotta."
     * - Non-empty changelog → preserved unchanged
     *
     * **Validates: Requirements 1.1, 1.2, 5.1**
     */
    @TestFactory
    fun `Property 1 - changelog normalization preserves non-empty and defaults empty`(): List<DynamicTest> =
        sampleVersions.map { version ->
            DynamicTest.dynamicTest(
                "UploadVersion(v=${version.version}, changelog=\"${version.changelog.take(20)}\") " +
                    "→ changelog ${if (version.changelog.isEmpty()) "defaults" else "preserved"}",
            ) {
                val operation = Operation.UploadVersion(version)
                val result = OperationPreprocessor.process(listOf(operation))
                val processed = (result[0] as Operation.UploadVersion).version

                if (version.changelog.isEmpty()) {
                    assertEquals(
                        "Uploaded via Terracotta.",
                        processed.changelog,
                        "Empty changelog should be set to default",
                    )
                } else {
                    assertEquals(
                        version.changelog,
                        processed.changelog,
                        "Non-empty changelog should be preserved",
                    )
                }
            }
        }

    /**
     * Property 2: Display Name Assignment
     *
     * For any TerracottaVersion processed through OperationPreprocessor,
     * displayName equals "Version {version_number}".
     *
     * **Validates: Requirements 2.1, 2.2, 5.2**
     */
    @TestFactory
    fun `Property 2 - displayName is always set to Version {version_number}`(): List<DynamicTest> =
        sampleVersions.map { version ->
            DynamicTest.dynamicTest(
                "UploadVersion(v=${version.version}) → displayName = \"Version ${version.version}\"",
            ) {
                val operation = Operation.UploadVersion(version)
                val result = OperationPreprocessor.process(listOf(operation))
                val processed = (result[0] as Operation.UploadVersion).version

                assertEquals(
                    "Version ${version.version}",
                    processed.displayName,
                    "displayName should always be 'Version {version_number}'",
                )
            }
        }

    /**
     * Property 3: Operation List Size Preservation
     *
     * For any list of operations, `process(ops).size == ops.size`.
     *
     * **Validates: Requirement 3.1**
     */
    @TestFactory
    fun `Property 3 - process preserves operation list size`(): List<DynamicTest> {
        val operationLists: List<Pair<String, List<Operation>>> =
            listOf(
                "empty list" to emptyList(),
                "single UploadVersion" to
                    listOf(
                        Operation.UploadVersion(sampleVersions[0]),
                    ),
                "single CreateProject" to
                    listOf(
                        Operation.CreateProject(sampleProject(sampleVersions.take(2))),
                    ),
                "single UpdateMetadata" to
                    listOf(
                        Operation.UpdateMetadata(
                            nameChanged = true,
                            summaryChanged = false,
                            licenseChanged = false,
                            newName = "New Name",
                            newSummary = "",
                            newLicense = "",
                        ),
                    ),
                "single UpdateDescription" to
                    listOf(
                        Operation.UpdateDescription("old", "new"),
                    ),
                "single UpdateTags" to
                    listOf(
                        Operation.UpdateTags(listOf("a"), listOf("b")),
                    ),
                "mixed operations (5)" to
                    listOf(
                        Operation.CreateProject(sampleProject(sampleVersions.take(1))),
                        Operation.UploadVersion(sampleVersions[1]),
                        Operation.UpdateMetadata(true, true, false, "n", "s", ""),
                        Operation.UpdateDescription("old", "new"),
                        Operation.UpdateTags(listOf("x"), listOf("y", "z")),
                    ),
                "multiple UploadVersion (10)" to
                    sampleVersions.take(10).map {
                        Operation.UploadVersion(it)
                    },
            )

        return operationLists.map { (name, ops) ->
            DynamicTest.dynamicTest("process($name) preserves size ${ops.size}") {
                val result = OperationPreprocessor.process(ops)
                assertEquals(
                    ops.size,
                    result.size,
                    "Operation list size should be preserved after preprocessing",
                )
            }
        }
    }

    /**
     * Property 4: Pass-Through for Non-Version Operations
     *
     * For UpdateMetadata, UpdateDescription, and UpdateTags operations,
     * preprocessed output equals input (identity transformation).
     *
     * **Validates: Requirement 3.3**
     */
    @TestFactory
    fun `Property 4 - non-version operations pass through unchanged`(): List<DynamicTest> {
        val nonVersionOperations: List<Pair<String, Operation>> =
            listOf(
                "UpdateMetadata(name only)" to
                    Operation.UpdateMetadata(
                        nameChanged = true,
                        summaryChanged = false,
                        licenseChanged = false,
                        newName = "Updated Plugin",
                        newSummary = "",
                        newLicense = "",
                    ),
                "UpdateMetadata(all fields)" to
                    Operation.UpdateMetadata(
                        nameChanged = true,
                        summaryChanged = true,
                        licenseChanged = true,
                        newName = "New Name",
                        newSummary = "New Summary",
                        newLicense = "Apache-2.0",
                    ),
                "UpdateDescription(short)" to
                    Operation.UpdateDescription(
                        oldDescription = "Old",
                        newDescription = "New",
                    ),
                "UpdateDescription(long)" to
                    Operation.UpdateDescription(
                        oldDescription = "A very long old description with lots of details",
                        newDescription = "A very long new description with even more details",
                    ),
                "UpdateTags(single to single)" to
                    Operation.UpdateTags(
                        oldTags = listOf("adventure"),
                        newTags = listOf("utility"),
                    ),
                "UpdateTags(multiple to multiple)" to
                    Operation.UpdateTags(
                        oldTags = listOf("combat", "pvp", "arena"),
                        newTags = listOf("pve", "survival", "economy"),
                    ),
                "UpdateTags(empty to some)" to
                    Operation.UpdateTags(
                        oldTags = emptyList(),
                        newTags = listOf("new-tag"),
                    ),
                "UpdateTags(some to empty)" to
                    Operation.UpdateTags(
                        oldTags = listOf("removed"),
                        newTags = emptyList(),
                    ),
            )

        return nonVersionOperations.map { (name, operation) ->
            DynamicTest.dynamicTest("$name passes through unchanged") {
                val result = OperationPreprocessor.process(listOf(operation))
                assertEquals(
                    operation,
                    result[0],
                    "Non-version operation should pass through unchanged",
                )
            }
        }
    }

    /**
     * Additional property: CreateProject normalizes all versions
     *
     * For CreateProject operations, all versions in the project are normalized.
     *
     * **Validates: Requirements 1.1, 1.2, 2.1, 2.2**
     */
    @TestFactory
    fun `Property - CreateProject normalizes all versions in project`(): List<DynamicTest> {
        val versionCounts = listOf(1, 2, 5)

        return versionCounts.map { count ->
            val versions = sampleVersions.take(count)
            DynamicTest.dynamicTest("CreateProject with $count version(s) normalizes all") {
                val project = sampleProject(versions)
                val operation = Operation.CreateProject(project)
                val result = OperationPreprocessor.process(listOf(operation))
                val processed = (result[0] as Operation.CreateProject).project

                assertEquals(count, processed.versions.size)
                processed.versions.forEachIndexed { index, processedVersion ->
                    val originalVersion = versions[index]

                    assertEquals(
                        "Version ${originalVersion.version}",
                        processedVersion.displayName,
                        "displayName should be set for version at index $index",
                    )

                    if (originalVersion.changelog.isEmpty()) {
                        assertEquals(
                            "Uploaded via Terracotta.",
                            processedVersion.changelog,
                            "Empty changelog should be defaulted for version at index $index",
                        )
                    } else {
                        assertEquals(
                            originalVersion.changelog,
                            processedVersion.changelog,
                            "Non-empty changelog should be preserved for version at index $index",
                        )
                    }
                }
            }
        }
    }

    private fun sampleProject(versions: List<TerracottaVersion>): TerracottaProject =
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
