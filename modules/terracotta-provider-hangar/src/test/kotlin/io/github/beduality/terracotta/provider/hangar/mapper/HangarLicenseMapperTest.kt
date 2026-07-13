package io.github.beduality.terracotta.provider.hangar.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HangarLicenseMapperTest {
    @ParameterizedTest
    @CsvSource(
        "MIT, MIT",
        "mit, MIT",
        "Apache-2.0, Apache 2.0",
        "apache-2.0, Apache 2.0",
        "GPL-3.0, GPL",
        "GPL-3.0-only, GPL",
        "GPL-2.0, GPL",
        "GPL-2.0-only, GPL",
        "LGPL-3.0, LGPL",
        "LGPL-3.0-only, LGPL",
        "LGPL-2.0, LGPL",
        "LGPL-2.0-only, LGPL",
        "AGPL-3.0, AGPL",
        "AGPL-3.0-only, AGPL",
        "CC0-1.0, Unspecified",
        "Unlicense, Unspecified",
        "BSD-3-Clause, Other",
        "Custom-License, Other",
    )
    fun `test toHangarLicense maps SPDX to Hangar values`(
        spdx: String,
        expected: String,
    ) {
        assertEquals(expected, HangarLicenseMapper.toHangarLicense(spdx))
    }

    @ParameterizedTest
    @CsvSource(
        "MIT, MIT",
        "Apache 2.0, Apache-2.0",
        "GPL, GPL",
        "LGPL, LGPL",
        "AGPL, AGPL",
        "Unspecified, UNLICENSED",
        "Other, Other",
        "Custom, Custom",
    )
    fun `test fromHangarLicense maps Hangar values back to canonical`(
        hangar: String,
        expected: String,
    ) {
        assertEquals(expected, HangarLicenseMapper.fromHangarLicense(hangar))
    }

    @Test
    fun `test fromHangarLicense preserves unknown custom licenses`() {
        assertEquals("Proprietary", HangarLicenseMapper.fromHangarLicense("Proprietary"))
    }
}
