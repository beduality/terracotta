package io.github.beduality.terracotta.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class TerracottaExtension {
    abstract val name: Property<String>
    abstract val summary: Property<String>
    abstract val description: Property<String>
    abstract val tags: ListProperty<String>
    abstract val license: Property<String>
    abstract val gameVersions: ListProperty<String>
    abstract val loaders: ListProperty<String>
    abstract val environment: Property<String>
    abstract val artifactFile: RegularFileProperty

    abstract val providers: NamedDomainObjectContainer<TerracottaProviderExtension>
}
