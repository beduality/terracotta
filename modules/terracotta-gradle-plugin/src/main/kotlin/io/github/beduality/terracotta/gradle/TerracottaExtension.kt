package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaReleaseType
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
    abstract val loaders: ListProperty<TerracottaLoader>
    abstract val environment: Property<TerracottaEnvironment>
    abstract val releaseType: Property<TerracottaReleaseType>
    abstract val changelog: Property<String>
    abstract val artifactFile: RegularFileProperty

    abstract val providers: NamedDomainObjectContainer<TerracottaProviderExtension>
}
