package io.github.beduality.terracotta.gradle

import org.gradle.api.Named
import org.gradle.api.provider.Property

/**
 * DSL block for configuring an individual registry provider.
 *
 * @see [Adding a Hangar provider](https://beduality.github.io/terracotta/content/integration/how-to-guides/adding-hangar-to-gradle-plugin.html)
 * @see [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
 */
abstract class TerracottaProviderExtension(private val name: String) : Named {
    override fun getName(): String = name

    /** Project ID on the provider registry. */
    abstract val projectId: Property<String>

    /** Authentication token for the provider registry. */
    abstract val token: Property<String>
}
