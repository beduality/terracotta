package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaCategory
import org.gradle.api.provider.Property

/**
 * DSL block for declaring a single [TerracottaCategory].
 *
 * The object name is used only as a DSL handle; the published identifier comes
 * from [id].
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 */
interface TerracottaCategoryExtension {
    /** Canonical category identifier. */
    val id: Property<String>

    /** Human-readable display name; defaults to [id]. */
    val displayName: Property<String>

    /** Builds the canonical category model from the configured DSL values. */
    fun toModel(): TerracottaCategory =
        TerracottaCategory(
            id = id.get(),
            displayName = displayName.orNull?.takeIf { it.isNotBlank() } ?: id.get(),
        )
}
