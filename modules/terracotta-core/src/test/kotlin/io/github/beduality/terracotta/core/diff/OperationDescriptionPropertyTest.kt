package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaVisibility
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

    private val visibilityPairs =
        TerracottaVisibility.entries.flatMap { old ->
            TerracottaVisibility.entries.filter { it != old }.map { new -> old to new }
        }

    private val categoryPairs =
        listOf(
            Pair(
                TerracottaProjectCategories(primary = TerracottaCategory("adventure", "Adventure")),
                TerracottaProjectCategories(
                    primary = TerracottaCategory("utility", "Utility"),
                    additional = listOf(TerracottaCategory("economy", "Economy")),
                ),
            ),
            Pair(
                TerracottaProjectCategories(
                    primary = TerracottaCategory("combat", "Combat"),
                    additional = listOf(TerracottaCategory("pvp", "PvP")),
                ),
                TerracottaProjectCategories(
                    primary = TerracottaCategory("pve", "PvE"),
                    additional = listOf(TerracottaCategory("survival", "Survival")),
                ),
            ),
            Pair(
                TerracottaProjectCategories(primary = TerracottaCategory("a", "A")),
                TerracottaProjectCategories(primary = TerracottaCategory("b", "B")),
            ),
            Pair(
                TerracottaProjectCategories(
                    primary = TerracottaCategory("tag1", "Tag 1"),
                    additional =
                        listOf(
                            TerracottaCategory("tag2", "Tag 2"),
                            TerracottaCategory("tag3", "Tag 3"),
                        ),
                ),
                TerracottaProjectCategories(primary = TerracottaCategory("tag4", "Tag 4")),
            ),
            Pair(
                TerracottaProjectCategories(primary = TerracottaCategory("kept", "Kept")),
                TerracottaProjectCategories(primary = TerracottaCategory("new-tag", "New Tag")),
            ),
            Pair(
                TerracottaProjectCategories(primary = TerracottaCategory("removed-tag", "Removed Tag")),
                TerracottaProjectCategories(primary = TerracottaCategory("kept", "Kept")),
            ),
            Pair(
                TerracottaProjectCategories(
                    primary = TerracottaCategory("alpha", "Alpha"),
                    additional = listOf(TerracottaCategory("beta", "Beta")),
                ),
                TerracottaProjectCategories(
                    primary = TerracottaCategory("gamma", "Gamma"),
                    additional =
                        listOf(
                            TerracottaCategory("delta", "Delta"),
                            TerracottaCategory("epsilon", "Epsilon"),
                        ),
                ),
            ),
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
                        categories = TerracottaProjectCategories(primary = TerracottaCategory("test", "Test")),
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
     * Property 6: UpdateCategories description content
     *
     * For any two distinct category values, description contains `~` and both old and new ids.
     */
    @TestFactory
    fun `Property 6 - UpdateCategories description contains tilde and both category lists`(): List<DynamicTest> =
        categoryPairs.map { (oldCategories, newCategories) ->
            DynamicTest.dynamicTest(
                "UpdateCategories(old=$oldCategories, new=$newCategories) description contains '~' and category values",
            ) {
                val operation = Operation.UpdateCategories(oldCategories, newCategories)
                val desc = operation.description

                assertTrue(
                    desc.contains("~"),
                    "UpdateCategories description should contain '~' but was: $desc",
                )
                oldCategories.allIds().forEach { id ->
                    assertTrue(
                        desc.contains(id),
                        "UpdateCategories description should contain old category '$id' but was: $desc",
                    )
                }
                newCategories.allIds().forEach { id ->
                    assertTrue(
                        desc.contains(id),
                        "UpdateCategories description should contain new category '$id' but was: $desc",
                    )
                }
            }
        }

    /**
     * Property 7: UpdateVisibility description content
     *
     * For any two distinct visibility values, description contains `~` and both old and new ids.
     */
    @TestFactory
    fun `Property 7 - UpdateVisibility description contains tilde and both visibility ids`(): List<DynamicTest> =
        visibilityPairs.map { (oldVisibility, newVisibility) ->
            DynamicTest.dynamicTest(
                "UpdateVisibility(old=${oldVisibility.id}, new=${newVisibility.id}) description contains '~' and visibility values",
            ) {
                val operation = Operation.UpdateVisibility(oldVisibility, newVisibility)
                val desc = operation.description

                assertTrue(
                    desc.contains("~"),
                    "UpdateVisibility description should contain '~' but was: $desc",
                )
                assertTrue(
                    desc.contains(oldVisibility.id),
                    "UpdateVisibility description should contain old visibility '${oldVisibility.id}' but was: $desc",
                )
                assertTrue(
                    desc.contains(newVisibility.id),
                    "UpdateVisibility description should contain new visibility '${newVisibility.id}' but was: $desc",
                )
            }
        }

    private fun TerracottaProjectCategories.allIds(): List<String> = listOf(primary.id) + additional.map { it.id }
}
