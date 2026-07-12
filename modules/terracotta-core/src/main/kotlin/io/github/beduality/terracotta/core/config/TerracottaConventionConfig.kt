package io.github.beduality.terracotta.core.config

/**
 * Convention configuration inside `terracotta.yml`.
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 */
data class TerracottaConventionConfig(
    /** Identifier of the convention used to interpret `README.md`. */
    val readme: String? = null,
    /** Identifier of the convention used to interpret `CHANGELOG.md`. */
    val changelog: String? = null,
)
