package io.github.beduality.terracotta.core.model.metadata.detector.adapters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameVersionNormalizerTest {
    @Test
    fun `returns empty list for unrecognizable input`() {
        assertTrue(GameVersionNormalizer.normalize("").isEmpty())
        assertTrue(GameVersionNormalizer.normalize("no version here").isEmpty())
        assertTrue(GameVersionNormalizer.normalize("com.example:foo:2.0.0").isEmpty())
    }

    @Test
    fun `extracts classic version from build metadata`() {
        assertEquals(listOf("1.20.1"), GameVersionNormalizer.normalize("1.20.1-R0.1-SNAPSHOT"))
    }

    @Test
    fun `extracts version from range strings`() {
        assertEquals(listOf("1.20.1"), GameVersionNormalizer.normalize("[1.20.1]"))
        assertEquals(listOf("1.20.1"), GameVersionNormalizer.normalize(">=1.20.1"))
        assertEquals(listOf("1.20.1", "1.20.2"), GameVersionNormalizer.normalize("[1.20.1,1.20.2)"))
    }

    @Test
    fun `extracts snapshot versions`() {
        assertEquals(listOf("25w14a"), GameVersionNormalizer.normalize("25w14a"))
        assertEquals(listOf("25w14a"), GameVersionNormalizer.normalize("minecraft_version=25w14a"))
    }

    @Test
    fun `extracts pre-release and release candidate versions`() {
        assertEquals(listOf("1.21.5-pre1"), GameVersionNormalizer.normalize("1.21.5-pre1"))
        assertEquals(listOf("1.21.5-pre1"), GameVersionNormalizer.normalize("1.21.5-pre1-R0.1-SNAPSHOT"))
        assertEquals(listOf("1.21.5-rc1"), GameVersionNormalizer.normalize("[1.21.5-rc1]"))
    }

    @Test
    fun `deduplicates multiple occurrences of the same version`() {
        assertEquals(listOf("1.20.1"), GameVersionNormalizer.normalize("1.20.1 and 1.20.1"))
    }
}
