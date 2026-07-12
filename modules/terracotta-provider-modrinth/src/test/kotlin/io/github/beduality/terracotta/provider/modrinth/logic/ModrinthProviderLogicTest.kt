package io.github.beduality.terracotta.provider.modrinth.logic

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModrinthProviderLogicTest {
    @Test
    fun `Modrinth loader mapper returns loader id unchanged`() {
        assertEquals("fabric", ModrinthProviderLogic.loaderMapper.mapToPlatform("fabric"))
        assertEquals("forge", ModrinthProviderLogic.loaderMapper.mapToPlatform("forge"))
    }

    @Test
    fun `Modrinth platform behavior is stateful and passes all operations through`() {
        assertTrue(ModrinthProviderLogic.platformBehavior.isStateful)

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
            )

        assertEquals(operations, ModrinthProviderLogic.platformBehavior.filterOperations(operations))
    }

    @Test
    fun `Modrinth loader mapper returns blank loader id unchanged`() {
        assertEquals("", ModrinthProviderLogic.loaderMapper.mapToPlatform(""))
    }
}
