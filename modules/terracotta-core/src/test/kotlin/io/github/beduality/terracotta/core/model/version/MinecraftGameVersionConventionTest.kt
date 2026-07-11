package io.github.beduality.terracotta.core.model.version

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MinecraftGameVersionConventionTest {
    private val convention = MinecraftGameVersionConvention

    @Test
    fun `parses classic release versions`() {
        assertEquals("1.20", convention.parse("1.20"))
        assertEquals("1.20.1", convention.parse("1.20.1"))
        assertEquals("1.21.4", convention.parse("1.21.4"))
    }

    @Test
    fun `parses snapshot versions`() {
        assertEquals("25w14a", convention.parse("25w14a"))
        assertEquals("16w20a", convention.parse("16w20a"))
        assertEquals("25w14a", convention.parse("25W14A"))
    }

    @Test
    fun `parses pre-release versions`() {
        assertEquals("1.21.5-pre1", convention.parse("1.21.5-pre1"))
        assertEquals("1.18-pre5", convention.parse("1.18-pre5"))
        assertEquals("1.21.5-pre1", convention.parse("1.21.5-PRE1"))
    }

    @Test
    fun `parses release candidate versions`() {
        assertEquals("1.21.5-rc1", convention.parse("1.21.5-rc1"))
        assertEquals("1.21.5-rc1", convention.parse("1.21.5-RC1"))
    }

    @Test
    fun `strips common packaging noise`() {
        assertEquals("1.20.1", convention.parse(">=1.20.1"))
        assertEquals("1.20.1", convention.parse("[1.20.1]"))
        assertEquals("1.20.1", convention.parse("(1.20.1)"))
        assertEquals("1.20.1", convention.parse("=1.20.1"))
        assertEquals("1.20.1", convention.parse("~1.20.1"))
        assertEquals("1.20.1", convention.parse("<1.20.1"))
    }

    @Test
    fun `returns null for invalid versions`() {
        assertNull(convention.parse("not-a-version"))
        assertNull(convention.parse("2.0"))
        assertNull(convention.parse("1"))
        assertNull(convention.parse(""))
        assertNull(convention.parse("1.20.1.2"))
    }

    @Test
    fun `resolver returns minecraft convention by default`() {
        assertEquals(MinecraftGameVersionConvention, GameVersionConventionResolver.resolve(null))
        assertEquals(MinecraftGameVersionConvention, GameVersionConventionResolver.resolve("minecraft"))
        assertEquals(MinecraftGameVersionConvention, GameVersionConventionResolver.resolve("MINECRAFT"))
    }

    @Test
    fun `resolver rejects unknown conventions`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameVersionConventionResolver.resolve("semver")
        }
    }
}
