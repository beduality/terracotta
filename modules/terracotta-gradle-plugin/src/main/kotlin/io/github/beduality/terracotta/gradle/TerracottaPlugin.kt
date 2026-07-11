package io.github.beduality.terracotta.gradle

import io.github.beduality.terracotta.core.config.TerracottaConfigLoader
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class TerracottaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("terracotta", TerracottaExtension::class.java)

        // Load terracotta.yml as default configuration. Missing values fall back to
        // built-in defaults, and anything set in the Kotlin DSL overrides the file.
        val config = TerracottaConfigLoader.load(File(project.projectDir, "terracotta.yml"))

        TerracottaExtensionConfigurer.configure(extension, config, project)
        TerracottaTaskRegistrar.registerTasks(extension, config, project)
    }
}
