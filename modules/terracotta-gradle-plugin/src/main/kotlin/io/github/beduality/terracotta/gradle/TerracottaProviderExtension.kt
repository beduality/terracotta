package io.github.beduality.terracotta.gradle

import org.gradle.api.Named
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class TerracottaProviderExtension(private val name: String) : Named {
    override fun getName(): String = name

    abstract val projectId: Property<String>
    abstract val token: Property<String>
}
