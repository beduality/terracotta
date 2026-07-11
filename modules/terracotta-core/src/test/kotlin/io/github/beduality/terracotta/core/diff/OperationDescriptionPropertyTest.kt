package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Property-based tests for Operation description formatting.
 *
 * Tests universal invariants across generated inputs without requiring
 * external PBT libraries — uses JUnit @TestFactory with DynamicTest.
 *
 * **Validates: Requirements 3.1, 3.2, 3.4**
 */
class OperationDescriptionPropertyTest {
    private val projectNames =
        listOf(
            "MyPlugin",
            "super-craft",
            "A Plugin With Spaces",
            "plugin_v2",
            "日本語プラグイン",
            "123numeric",
            "special!@#chars",
            "MixedCASE-Name",
            "x",
            "a-very-long-project-name-that-goes-on-and-on",
        )

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

    private val tagListPairs =
        listOf(
            Pair(listOf("adventure"), listOf("utility", "economy")),
            Pair(listOf("combat", "pvp"), listOf("pve", "survival")),
            Pair(listOf("a"), listOf("b")),
            Pair(listOf("tag1", "tag2", "tag3"), listOf("tag4")),
            Pair(emptyList(), listOf("new-tag")),
            Pair(listOf("removed-tag"), emptyList()),
            Pair(listOf("alpha", "beta"), listOf("gamma", "delta", "epsilon")),
        )

    /**
     * Property 4: CreateProject description format
     *
     * For any project with non-empty name, description contains `+` and the name.
     */
    @TestFactory
    fun `Property 4 - CreateProject description contains plus and project name`(): List<DynamicTest> =
        projectNames.map { name ->
            DynamicTest.dynamicTest("CreateProject(name=\"$name\") description contains '+' and name") {
                val project =
                    TerracottaProject(
                        id = "test-id",
                        name = name,
                        summary = "A test project",
                        description = "Description",
                        versions = emptyList(),
                        tags = emptyList(),
                        license = "MIT",
                    )
                val operation = Operation.CreateProject(project)
                val desc = operation.description

                assertTrue(
                    desc.contains("+"),
                    "CreateProject description should contain '+' but was: $desc",
                )
                assertTrue(
                    desc.contains(name),
                    "CreateProject description should contain project name '$name' but was: $desc",
                )
            }
        }

    /**
     * Property 5: UploadVersion description format
     *
     * For any version with non-empty version string, description contains `+` and the version.
     */
    @TestFactory
    fun `Property 5 - UploadVersion description contains plus and version string`(): List<DynamicTest> =
        versionStrings.map { versionStr ->
            DynamicTest.dynamicTest("UploadVersion(version=\"$versionStr\") description contains '+' and version") {
                val version =
                    TerracottaVersion(
                        version = versionStr,
                        artifactPath = "/path/to/artifact.jar",
                        gameVersions = listOf("1.20.4"),
                    )
                val operation = Operation.UploadVersion(version)
                val desc = operation.description

                assertTrue(
                    desc.contains("+"),
                    "UploadVersion description should contain '+' but was: $desc",
                )
                assertTrue(
                    desc.contains(versionStr),
                    "UploadVersion description should contain version '$versionStr' but was: $desc",
                )
            }
        }

    /**
     * Property 6: UpdateTags description content
     *
     * For any two distinct tag lists, description contains `~` and both old and new values.
     */
    @TestFactory
    fun `Property 6 - UpdateTags description contains tilde and both tag lists`(): List<DynamicTest> =
        tagListPairs.map { (oldTags, newTags) ->
            DynamicTest.dynamicTest(
                "UpdateTags(old=$oldTags, new=$newTags) description contains '~' and tag values",
            ) {
                val operation = Operation.UpdateTags(oldTags, newTags)
                val desc = operation.description

                assertTrue(
                    desc.contains("~"),
                    "UpdateTags description should contain '~' but was: $desc",
                )
                oldTags.forEach { tag ->
                    assertTrue(
                        desc.contains(tag),
                        "UpdateTags description should contain old tag '$tag' but was: $desc",
                    )
                }
                newTags.forEach { tag ->
                    assertTrue(
                        desc.contains(tag),
                        "UpdateTags description should contain new tag '$tag' but was: $desc",
                    )
                }
            }
        }
}
