package io.github.beduality.terracotta.gradle

import org.gradle.api.provider.Property

/**
 * Nested DSL block for README and changelog conventions.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/conventions.html)
 */
abstract class TerracottaConventionExtension {
    /** Convention identifier used to interpret `README.md`. */
    abstract val readme: Property<String>

    /** Convention identifier used to interpret `CHANGELOG.md`. */
    abstract val changelog: Property<String>
}
