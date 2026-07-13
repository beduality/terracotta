package io.github.beduality.terracotta.provider.hangar.mapper

/**
 * Maps between Terracotta's canonical SPDX license identifiers and the
 * license strings accepted by Hangar's project settings API.
 *
 * Hangar's license dropdown offers a fixed set of values:
 * `Unspecified`, `MIT`, `Apache 2.0`, `GPL`, `LGPL`, `AGPL`, and `Other`.
 * Custom identifiers are sent as `Other`.
 *
 * @see [Hangar provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-hangar/tutorials/using-hangar.html)
 */
internal object HangarLicenseMapper {
    /**
     * Converts a Terracotta [spdxId] to the closest Hangar license value.
     *
     * Matching is case-insensitive. Unknown or custom identifiers fall back
     * to `Other` so Hangar can display them as a custom license.
     */
    fun toHangarLicense(spdxId: String): String =
        when (spdxId.uppercase()) {
            "MIT" -> "MIT"
            "APACHE-2.0" -> "Apache 2.0"
            "GPL-3.0", "GPL-3.0-ONLY", "GPL-2.0", "GPL-2.0-ONLY" -> "GPL"
            "LGPL-3.0", "LGPL-3.0-ONLY", "LGPL-2.0", "LGPL-2.0-ONLY" -> "LGPL"
            "AGPL-3.0", "AGPL-3.0-ONLY" -> "AGPL"
            "UNLICENSE", "CC0-1.0" -> "Unspecified"
            else -> "Other"
        }

    /**
     * Converts a Hangar [hangarLicense] value back to a Terracotta license
     * identifier.
     *
     * Well-known Hangar values are mapped to common SPDX-style identifiers.
     * Values that cannot be mapped (including `Other` and unknown custom
     * values) are returned as-is so the provider does not lose information.
     */
    fun fromHangarLicense(hangarLicense: String): String =
        when (hangarLicense) {
            "MIT" -> "MIT"
            "Apache 2.0" -> "Apache-2.0"
            "GPL" -> "GPL"
            "LGPL" -> "LGPL"
            "AGPL" -> "AGPL"
            "Unspecified" -> "UNLICENSED"
            else -> hangarLicense
        }
}
