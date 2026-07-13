package io.github.beduality.terracotta.core.provider.logic

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.test.CapturingLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlatformBehaviorTest {
    @Test
    fun `stateful platform keeps all operations`() {
        val behavior =
            object : PlatformBehavior {
                override val isStateful: Boolean = true
            }
        val operations =
            listOf(
                Operation.UpdateDescription("old", "new"),
                Operation.UploadVersion(
                    TerracottaVersion(
                        version = "1.0.0",
                        artifactPath = "",
                        gameVersions = emptyList(),
                        releaseType = TerracottaReleaseType.RELEASE,
                    ),
                ),
            )

        assertEquals(operations, behavior.filterOperations(operations))
    }

    @Test
    fun `append-only platform keeps only UploadVersion`() {
        val behavior =
            object : PlatformBehavior {
                override val isStateful: Boolean = false
            }
        val version =
            TerracottaVersion(
                version = "1.0.0",
                artifactPath = "",
                gameVersions = emptyList(),
                releaseType = TerracottaReleaseType.RELEASE,
            )
        val operations =
            listOf(
                Operation.UpdateDescription("old", "new"),
                Operation.UploadVersion(version),
                Operation.UpdateCategories(
                    TerracottaProjectCategories(primary = TerracottaCategory("old", "Old")),
                    TerracottaProjectCategories(primary = TerracottaCategory("tag", "Tag")),
                ),
            )

        assertEquals(listOf(Operation.UploadVersion(version)), behavior.filterOperations(operations))
    }

    @Test
    fun `partition returns applied and skipped operations`() {
        val behavior =
            object : PlatformBehavior {
                override val isStateful: Boolean = false
            }
        val version =
            TerracottaVersion(
                version = "1.0.0",
                artifactPath = "",
                gameVersions = emptyList(),
                releaseType = TerracottaReleaseType.RELEASE,
            )
        val description = Operation.UpdateDescription("old", "new")
        val upload = Operation.UploadVersion(version)
        val operations = listOf(description, upload)

        val (applied, skipped) = behavior.partition(operations)

        assertEquals(listOf(upload), applied)
        assertEquals(listOf(description), skipped)
    }

    @Test
    fun `partition preserves duplicate skipped operations`() {
        val behavior =
            object : PlatformBehavior {
                override val isStateful: Boolean = false
            }
        val description1 = Operation.UpdateDescription("old", "new")
        val description2 = Operation.UpdateDescription("older", "newer")
        val operations = listOf(description1, description2)

        val (applied, skipped) = behavior.partition(operations)

        assertEquals(emptyList<Operation>(), applied)
        assertEquals(listOf(description1, description2), skipped)
    }

    @Test
    fun `filterAndWarn returns applied operations and logs skipped operations`() {
        val behavior =
            object : PlatformBehavior {
                override val isStateful: Boolean = false
            }
        val version =
            TerracottaVersion(
                version = "1.0.0",
                artifactPath = "",
                gameVersions = emptyList(),
                releaseType = TerracottaReleaseType.RELEASE,
            )
        val description1 = Operation.UpdateDescription("old", "new")
        val description2 = Operation.UpdateDescription("older", "newer")
        val upload = Operation.UploadVersion(version)
        val operations = listOf(description1, upload, description2)
        val logger = CapturingLogger()

        val applied = behavior.filterAndWarn(operations, logger, "test")

        assertEquals(listOf(upload), applied)
        assertEquals(1, logger.warnings.size)
        assertTrue(logger.warnings.first().contains("Platform 'test' does not support operation"))
        assertTrue(logger.warnings.first().contains("2 occurrence(s) will be skipped"))
    }
}
