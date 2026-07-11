package io.github.beduality.terracotta.gradle

import org.gradle.api.provider.Property

/**
 * Nested DSL block for README and changelog conventions.
 */
abstract class TerracottaConventionExtension {
    abstract val readme: Property<String>
    abstract val changelog: Property<String>
}
