package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.TerracottaConfigLoader
import io.github.beduality.terracotta.core.state.FileSystemStateSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Entry point for the Terracotta Gradle plugin.
 *
 * Registers the `terracotta` extension and the `terracottaPlan`, `terracottaApply`,
 * and `terracottaDestroy` tasks for each configured provider.
 *
 * @see [Getting started tutorial](https://beduality.github.io/terracotta/content/modules/gradle-plugin/tutorials/getting-started.html)
 * @see [Installation guide](https://beduality.github.io/terracotta/content/modules/gradle-plugin/tutorials/installation.html)
 */
class TerracottaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("terracotta", TerracottaExtension::class.java)

        // Load terracotta.yml as default configuration. Missing values fall back to
        // built-in defaults, and anything set in the Kotlin DSL overrides the file.
        val config = TerracottaConfigLoader.load(File(project.projectDir, "terracotta.yml"))

        extension.stateFile.convention(
            project.layout.projectDirectory.file(FileSystemStateSource.DEFAULT_FILE_NAME),
        )

        TerracottaExtensionConfigurer.configure(extension, config, project)
        TerracottaTaskRegistrar.registerTasks(extension, config, project)
    }
}
