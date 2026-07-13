package io.github.beduality.terracotta.gradle

import org.gradle.api.Named
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * A single gallery image entry in the Gradle DSL.
 *
 * The object name is used only as a DSL handle; the published title comes from
 * [title]. The optional [key] provides a stable local identity that survives
 * title changes and file moves.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 */
abstract class TerracottaGalleryExtension(private val name: String) : Named {
    override fun getName(): String = name

    /** Image file to upload. */
    abstract val imageFile: RegularFileProperty

    /** Optional stable local identity key. */
    abstract val key: Property<String>

    /** Human-readable title used as the stable identity key. */
    abstract val title: Property<String>

    /** Optional longer description. */
    abstract val description: Property<String>

    /** Whether the image should be highlighted by the provider. */
    abstract val featured: Property<Boolean>

    /** Display order; lower values come first. */
    abstract val ordering: Property<Int>
}
