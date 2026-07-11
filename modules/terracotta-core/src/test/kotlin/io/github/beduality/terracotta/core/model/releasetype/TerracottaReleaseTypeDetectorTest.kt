package io.github.beduality.terracotta.core.model.releasetype

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TerracottaReleaseTypeDetectorTest {
    @Test
    fun `detects release`() {
        assertEquals(TerracottaReleaseType.RELEASE, detectReleaseType("1.0.0"))
        assertEquals(TerracottaReleaseType.RELEASE, detectReleaseType("v2.3.4"))
    }

    @Test
    fun `detects alpha`() {
        assertEquals(TerracottaReleaseType.ALPHA, detectReleaseType("1.0.0-alpha"))
        assertEquals(TerracottaReleaseType.ALPHA, detectReleaseType("1.0.0-alpha.1"))
    }

    @Test
    fun `detects beta`() {
        assertEquals(TerracottaReleaseType.BETA, detectReleaseType("1.0.0-beta"))
        assertEquals(TerracottaReleaseType.BETA, detectReleaseType("1.0.0-rc1"))
        assertEquals(TerracottaReleaseType.BETA, detectReleaseType("1.0.0-SNAPSHOT"))
    }

    @Test
    fun `returns null for unspecified`() {
        assertNull(detectReleaseType("unspecified"))
    }

    @Test
    fun `returns null for unparseable version`() {
        assertNull(detectReleaseType("not-a-version"))
    }
}
