package io.github.beduality.terracotta.core.model.version

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SemverVersionConventionTest {
    @Test
    fun `parse returns SemVer for valid version`() {
        val result = SemverVersionConvention.parse("1.2.3")

        assertNotNull(result)
        assertEquals(1, result?.major)
        assertEquals(2, result?.minor)
        assertEquals(3, result?.patch)
    }

    @Test
    fun `parse strips leading v prefix`() {
        val result = SemverVersionConvention.parse("v1.0.0")

        assertNotNull(result)
        assertEquals(1, result?.major)
    }

    @Test
    fun `parse strips leading V prefix`() {
        val result = SemverVersionConvention.parse("V2.0.0")

        assertNotNull(result)
        assertEquals(2, result?.major)
    }

    @Test
    fun `parse returns null for invalid version`() {
        assertNull(SemverVersionConvention.parse("not-a-version"))
    }
}
