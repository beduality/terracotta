package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Nested

/**
 * Nested `categories { ... }` DSL block inside the top-level `terracotta` extension.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 */
abstract class TerracottaCategoriesExtension {
    @get:Nested
    /** Primary project category. */
    abstract val primary: TerracottaCategoryExtension

    @get:Nested
    /** Additional project categories. */
    abstract val additional: NamedDomainObjectContainer<TerracottaCategoryExtension>

    /** Configures the primary category. */
    fun primary(action: Action<in TerracottaCategoryExtension>) {
        action.execute(primary)
    }

    /** Adds an additional category. */
    fun additional(
        id: String,
        action: Action<in TerracottaCategoryExtension>,
    ) {
        additional.create(id) { extension ->
            extension.id.set(id)
            action.execute(extension)
        }
    }

    /** Builds the canonical project categories model from the configured DSL values. */
    fun toModel(): TerracottaProjectCategories =
        TerracottaProjectCategories(
            primary = primary.toModel(),
            additional = additional.map { it.toModel() },
        )
}
