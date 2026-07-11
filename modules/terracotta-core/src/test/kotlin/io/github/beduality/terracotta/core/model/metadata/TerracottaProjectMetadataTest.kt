package io.github.beduality.terracotta.core.model.metadata

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TerracottaProjectMetadataTest {
    @Test
    fun `defaults are null`() {
        val metadata = TerracottaProjectMetadata()

        assertNull(metadata.name)
        assertNull(metadata.summary)
        assertNull(metadata.description)
        assertNull(metadata.license)
        assertNull(metadata.gameVersions)
        assertNull(metadata.loaders)
        assertNull(metadata.environment)
        assertNull(metadata.releaseType)
    }

    @Test
    fun `merge prefers first non-null scalar`() {
        val first = TerracottaProjectMetadata(name = "First", license = "MIT")
        val second = TerracottaProjectMetadata(name = "Second", license = "Apache-2.0", summary = "Summary")

        val merged = first.merge(second)

        assertEquals("First", merged.name)
        assertEquals("MIT", merged.license)
        assertEquals("Summary", merged.summary)
    }

    @Test
    fun `merge fills missing scalars from second`() {
        val first = TerracottaProjectMetadata(name = "Only")
        val second = TerracottaProjectMetadata(summary = "Summary", license = "MIT")

        val merged = first.merge(second)

        assertEquals("Only", merged.name)
        assertEquals("Summary", merged.summary)
        assertEquals("MIT", merged.license)
    }

    @Test
    fun `merge combines lists with distinct entries`() {
        val first =
            TerracottaProjectMetadata(
                gameVersions = listOf("1.21.1", "1.21.2"),
                loaders = listOf("fabric"),
            )
        val second =
            TerracottaProjectMetadata(
                gameVersions = listOf("1.21.2", "1.21.3"),
                loaders = listOf("forge"),
            )

        val merged = first.merge(second)

        assertEquals(listOf("1.21.1", "1.21.2", "1.21.3"), merged.gameVersions)
        assertEquals(listOf("fabric", "forge"), merged.loaders)
    }

    @Test
    fun `merge keeps non-null list when other list is null`() {
        val first = TerracottaProjectMetadata(loaders = listOf("paper"))
        val second = TerracottaProjectMetadata()

        val merged = first.merge(second)

        assertEquals(listOf("paper"), merged.loaders)
    }

    @Test
    fun `merge takes second list when first list is null`() {
        val first = TerracottaProjectMetadata()
        val second = TerracottaProjectMetadata(loaders = listOf("fabric"))

        val merged = first.merge(second)

        assertEquals(listOf("fabric"), merged.loaders)
    }

    @Test
    fun `merge combines enums with first winning`() {
        val first = TerracottaProjectMetadata(environment = TerracottaEnvironment.SERVER_ONLY)
        val second = TerracottaProjectMetadata(environment = TerracottaEnvironment.UNIVERSAL)

        val merged = first.merge(second)

        assertEquals(TerracottaEnvironment.SERVER_ONLY, merged.environment)
    }
}
