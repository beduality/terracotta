package io.github.beduality.terracotta.core.model.projectfile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LicenseFileTest {
    @Test
    fun `detects MIT`() {
        val file = LicenseFile("MIT License\n\nCopyright (c) 2026")

        assertEquals("MIT", file.licenseId)
    }

    @Test
    fun `detects Apache-2_0`() {
        val file = LicenseFile("Apache License\nVersion 2.0")

        assertEquals("Apache-2.0", file.licenseId)
    }

    @Test
    fun `detects GPL-3_0`() {
        val file = LicenseFile("GNU GENERAL PUBLIC LICENSE\nVersion 3")

        assertEquals("GPL-3.0", file.licenseId)
    }

    @Test
    fun `detects MPL-2_0`() {
        val file = LicenseFile("Mozilla Public License\nVersion 2.0")

        assertEquals("MPL-2.0", file.licenseId)
    }

    @Test
    fun `detects BSD-3-Clause`() {
        val file = LicenseFile("BSD 3-CLAUSE")

        assertEquals("BSD-3-Clause", file.licenseId)
    }

    @Test
    fun `detects Unlicense`() {
        val file = LicenseFile("This is free and unencumbered software released into the public domain.\nUNLICENSE")

        assertEquals("Unlicense", file.licenseId)
    }

    @Test
    fun `returns null for unknown license`() {
        val file = LicenseFile("Some custom license text")

        assertNull(file.licenseId)
    }

    @Test
    fun `returns null when content is missing`() {
        val file = LicenseFile(null)

        assertNull(file.licenseId)
    }
}
