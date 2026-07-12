package io.github.beduality.terracotta.core.config

/**
 * Provider-specific configuration inside `terracotta.yml`.
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 */
data class TerracottaProviderConfig(
    /** Project ID on the provider registry. */
    val projectId: String? = null,
    /** Authentication token for the provider registry. */
    val token: String? = null,
)
