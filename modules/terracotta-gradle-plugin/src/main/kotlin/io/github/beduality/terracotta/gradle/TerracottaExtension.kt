package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Top-level `terracotta { ... }` DSL extension.
 *
 * Properties set here override values from `terracotta.yml`. Convention values are
 * applied when neither the DSL nor the file provides a value.
 *
 * @see [Kotlin DSL configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
 * @see [Config schema reference](https://beduality.github.io/terracotta/content/modules/core/reference/config-schema.html)
 */
abstract class TerracottaExtension {
    /** Project display name. */
    abstract val name: Property<String>

    /** Short project summary or tagline. */
    abstract val summary: Property<String>

    /** Full project description. */
    abstract val description: Property<String>

    /** Search tags. */
    abstract val tags: ListProperty<String>

    /** SPDX license identifier. */
    abstract val license: Property<String>

    /** Optional URL to the full license text. */
    abstract val licenseUrl: Property<String>

    /** Project icon file to upload. */
    abstract val icon: RegularFileProperty

    /** Supported Minecraft game versions. */
    abstract val gameVersions: ListProperty<String>

    /** Supported loader identifiers. */
    abstract val loaders: ListProperty<String>

    /** Runtime environment. */
    abstract val environment: Property<TerracottaEnvironment>

    /** Release type. */
    abstract val releaseType: Property<TerracottaReleaseType>

    /** Release notes for the current version. */
    abstract val changelog: Property<String>

    @get:Nested
    /** README and changelog convention identifiers. */
    abstract val conventions: TerracottaConventionExtension

    @get:Nested
    /** Canonical project links. */
    abstract val links: TerracottaLinksExtension

    /** Configures the canonical project links. */
    fun links(action: Action<in TerracottaLinksExtension>) {
        action.execute(links)
    }

    /** Compiled artifact to upload. */
    abstract val artifactFile: RegularFileProperty

    /** Gallery images for the project. */
    abstract val gallery: NamedDomainObjectContainer<TerracottaGalleryExtension>

    /** Per-provider configuration. */
    abstract val providers: NamedDomainObjectContainer<TerracottaProviderExtension>

    /**
     * File used to persist Terracotta run state between executions.
     *
     * Defaults to `.terracotta-state.yml` in the project directory.
     *
     * @see [Kotlin DSL Configuration](https://beduality.github.io/terracotta/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.html)
     */
    abstract val stateFile: RegularFileProperty
}
