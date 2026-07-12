package io.github.beduality.terracotta.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

private fun File.writeTerracottaYml() {
    File(this, "terracotta.yml").writeText(
        """
        name: "YAML Name"
        summary: "Lightweight Paper plugin"
        description: "A useful plugin."
        tags:
          - paper
        license: MIT
        gameVersions:
          - 1.21.8
        loaders:
          - paper
        environment: server_only
        releaseType: release
        changelog: "Initial release"
        providers:
          modrinth:
            projectId: my-plugin
        """.trimIndent(),
    )
}

private fun File.writeSettings() {
    File(this, "settings.gradle.kts").writeText(
        """
        rootProject.name = "integration-test"
        """.trimIndent(),
    )
}

private fun pluginClasspathWithoutFileSystem(): List<File> {
    val properties = Properties()
    val resource =
        TerracottaPluginIntegrationTest::class.java.classLoader
            .getResource("plugin-under-test-metadata.properties")
            ?: error("plugin-under-test-metadata.properties not found")
    resource.openStream().use { properties.load(it) }
    val classpath =
        properties.getProperty("implementation-classpath")
            ?: error("implementation-classpath not found")
    return classpath
        .split(File.pathSeparatorChar)
        .map { File(it) }
        .filter { it.exists() }
        .filterNot { "terracotta-state-filesystem" in it.name }
}

class TerracottaPluginIntegrationTest {
    @Test
    fun `auto detects name from project when terracotta yml is missing name`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printName") {
                doLast {
                    println("NAME=" + terracotta.name.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printName")
                .build()

        assertTrue("NAME=integration-test" in result.output, "Expected name from project settings")
    }

    @Test
    fun `auto detects description and summary from README`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "README.md").writeText(
            """
            # My Plugin

            A short summary of the plugin.

            Longer description here.
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printDescription") {
                doLast {
                    println("DESC=" + terracotta.description.get())
                    println("SUMMARY=" + terracotta.summary.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printDescription")
                .build()

        assertTrue("DESC=# My Plugin" in result.output, "Expected full README as description")
        assertTrue("SUMMARY=A short summary of the plugin." in result.output, "Expected first paragraph as summary")
    }

    @Test
    fun `auto detects changelog from CHANGELOG for project version`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "gradle.properties").writeText("version = 1.2.0")
        File(projectDir, "CHANGELOG.md").writeText(
            """
            # Changelog

            ## [Unreleased]

            - future

            ## [1.2.0]

            ### Fixed
            - a bug

            ## [1.1.0]

            - old change
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printChangelog") {
                doLast {
                    println("CHANGELOG=" + terracotta.changelog.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printChangelog")
                .build()

        assertTrue("CHANGELOG=### Fixed\n- a bug" in result.output, "Expected changelog for version 1.2.0")
    }

    @Test
    fun `kotlin dsl overrides auto detected values`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "README.md").writeText(
            """
            # README Name

            README summary.
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                name.set("DSL Name")
                summary.set("DSL Summary")
            }

            tasks.register("printMetadata") {
                doLast {
                    println("NAME=" + terracotta.name.get())
                    println("SUMMARY=" + terracotta.summary.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printMetadata")
                .build()

        assertTrue("NAME=DSL Name" in result.output, "Expected DSL to override detected name")
        assertTrue("SUMMARY=DSL Summary" in result.output, "Expected DSL to override detected summary")
    }

    @Test
    fun `registers provider tasks from terracotta yml`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            name: "My Plugin"
            summary: "Lightweight Paper plugin"
            description: "A useful plugin."
            tags:
              - paper
            license: MIT
            gameVersions:
              - 1.21.8
            loaders:
              - paper
            environment: server_only
            releaseType: release
            changelog: "Initial release"
            providers:
              modrinth:
                projectId: my-plugin
            """.trimIndent(),
        )

        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "integration-test"
            """.trimIndent(),
        )

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("tasks", "--all", "--stacktrace")
                .build()

        val output = result.output
        assertTrue("terracottaPlanModrinth" in output, "Expected terracottaPlanModrinth task to be registered")
        assertTrue("terracottaApplyModrinth" in output, "Expected terracottaApplyModrinth task to be registered")
        assertTrue(result.tasks.any { it.path == ":tasks" && it.outcome == TaskOutcome.SUCCESS })
    }

    @Test
    fun `loads name from terracotta yml`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeTerracottaYml()
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printName") {
                doLast {
                    println("NAME=" + terracotta.name.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printName")
                .build()

        assertTrue("NAME=YAML Name" in result.output, "Expected name from terracotta.yml")
    }

    @Test
    fun `kotlin dsl overrides terracotta yml`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeTerracottaYml()
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                name.set("DSL Name")
            }

            tasks.register("printName") {
                doLast {
                    println("NAME=" + terracotta.name.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printName")
                .build()

        assertTrue("NAME=DSL Name" in result.output, "Expected DSL to override terracotta.yml")
    }

    @Test
    fun `loads licenseUrl from terracotta yml`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            license: MIT
            licenseUrl: https://yaml.example.com/LICENSE
            """.trimIndent(),
        )
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printLicenseUrl") {
                doLast {
                    println("LICENSE_URL=" + terracotta.licenseUrl.orNull)
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printLicenseUrl")
                .build()

        assertTrue("LICENSE_URL=https://yaml.example.com/LICENSE" in result.output, "Expected licenseUrl from terracotta.yml")
    }

    @Test
    fun `kotlin dsl overrides licenseUrl from terracotta yml`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            license: MIT
            licenseUrl: https://yaml.example.com/LICENSE
            """.trimIndent(),
        )
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                licenseUrl.set("https://dsl.example.com/LICENSE")
            }

            tasks.register("printLicenseUrl") {
                doLast {
                    println("LICENSE_URL=" + terracotta.licenseUrl.orNull)
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printLicenseUrl")
                .build()

        assertTrue("LICENSE_URL=https://dsl.example.com/LICENSE" in result.output, "Expected DSL to override terracotta.yml licenseUrl")
    }

    @Test
    fun `loads gallery from terracotta yml and dsl`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            gallery:
              - path: docs/assets/yaml-image.png
                title: YAML Image
                ordering: 1
            """.trimIndent(),
        )
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                gallery {
                    register("dslImage") {
                        imageFile.set(file("docs/assets/dsl-image.png"))
                        title.set("DSL Image")
                        ordering.set(2)
                    }
                }
            }

            tasks.register("printGallery") {
                doLast {
                    terracotta.gallery.forEach { item ->
                        println("GALLERY=" + item.title.get() + ":" + item.imageFile.get().asFile.name)
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printGallery")
                .build()

        assertTrue("GALLERY=YAML Image:yaml-image.png" in result.output, "Expected YAML gallery item")
        assertTrue("GALLERY=DSL Image:dsl-image.png" in result.output, "Expected DSL gallery item")
    }

    @Test
    fun `loads icon from terracotta yml and dsl override`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            icon: docs/assets/yaml-icon.png
            """.trimIndent(),
        )
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                icon.set(file("docs/assets/dsl-icon.png"))
            }

            tasks.register("printIcon") {
                doLast {
                    println("ICON=" + terracotta.icon.orNull?.asFile?.name)
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printIcon")
                .build()

        assertTrue("ICON=dsl-icon.png" in result.output, "Expected DSL to override YAML icon")
    }

    @Test
    fun `loads links from terracotta yml and dsl`(
        @TempDir projectDir: File,
    ) {
        File(projectDir, "terracotta.yml").writeText(
            """
            links:
              homepage: "https://yaml.example.com"
              source: "https://github.com/yaml/project"
              donations:
                - platform: patreon
                  url: "https://patreon.com/yaml"
            """.trimIndent(),
        )
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                links {
                    issues.set("https://github.com/dsl/project/issues")
                    donation("ko-fi", "https://ko-fi.com/dsl")
                    other("twitter", "https://twitter.com/dsl")
                }
            }

            tasks.register("printLinks") {
                doLast {
                    println("HOMEPAGE=" + terracotta.links.homepage.orNull)
                    println("SOURCE=" + terracotta.links.source.orNull)
                    println("ISSUES=" + terracotta.links.issues.orNull)
                    println("DONATIONS=" + terracotta.links.donations.get().joinToString { it.platform + ":" + it.url })
                    println("OTHER=" + terracotta.links.other.get().map { it.key + ":" + it.value }.joinToString())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printLinks")
                .build()

        assertTrue("HOMEPAGE=https://yaml.example.com" in result.output, "Expected homepage from YAML")
        assertTrue("SOURCE=https://github.com/yaml/project" in result.output, "Expected source from YAML")
        assertTrue("ISSUES=https://github.com/dsl/project/issues" in result.output, "Expected issues from DSL")
        assertTrue("patreon:https://patreon.com/yaml" in result.output, "Expected YAML donation")
        assertTrue("ko-fi:https://ko-fi.com/dsl" in result.output, "Expected DSL donation")
        assertTrue("OTHER=twitter:https://twitter.com/dsl" in result.output, "Expected other from DSL")
    }

    @Test
    fun `state file defaults to terracotta-state yml in project directory`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printStateFile") {
                doLast {
                    println("STATE_FILE=" + terracotta.stateFile.get().asFile.name)
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printStateFile")
                .build()

        assertTrue("STATE_FILE=.terracotta-state.yml" in result.output, "Expected default state file name")
    }

    @Test
    fun `state file can be overridden through dsl`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                stateFile.set(file("custom-state.yml"))
            }

            tasks.register("printStateFile") {
                doLast {
                    println("STATE_FILE=" + terracotta.stateFile.get().asFile.name)
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printStateFile")
                .build()

        assertTrue("STATE_FILE=custom-state.yml" in result.output, "Expected DSL state file override")
    }

    @Test
    fun `state source defaults to filesystem backend`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printStateSource") {
                doLast {
                    println("STATE_SOURCE=" + terracotta.stateSource.get())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printStateSource")
                .build()

        assertTrue("STATE_SOURCE=filesystem" in result.output, "Expected default state source to be filesystem")
    }

    @Test
    fun `state source settings can override filesystem path`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                stateSourceSettings.put("path", "custom-state.yml")
            }

            tasks.register("printStatePath") {
                doLast {
                    println("STATE_PATH=" + terracotta.stateSourceSettings.get()["path"])
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printStatePath")
                .build()

        assertTrue("STATE_PATH=custom-state.yml" in result.output, "Expected state source path override")
    }

    @Test
    fun `unknown state source fails with descriptive message`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                stateSource.set("unknown")
            }

            tasks.register("useState") {
                doLast { }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("useState")
                .buildAndFail()

        assertTrue("unknown" in result.output, "Expected error message to mention unknown backend")
    }

    @Test
    fun `fails cleanly when filesystem backend is missing from plugin classpath`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("useState") {
                doLast { }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath(pluginClasspathWithoutFileSystem())
                .withArguments("useState")
                .buildAndFail()

        val output = result.output
        assertTrue(
            "No state source factory found with id 'filesystem'" in output,
            "Expected missing backend error, output was:\n$output",
        )
        assertTrue(
            "terracotta-state-filesystem" in output,
            "Expected dependency hint for filesystem backend, output was:\n$output",
        )
        assertTrue(
            "NoClassDefFoundError" !in output,
            "Expected a clean GradleException, not a class-loading failure, output was:\n$output",
        )
    }

    @Test
    fun `state file convenience behaves like filesystem backend`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                stateFile.set(file("custom-state.yml"))
            }

            tasks.register("printStateSource") {
                doLast {
                    println("STATE_SOURCE=" + terracotta.stateSource.orNull)
                    println("STATE_PATH=" + terracotta.stateSourceSettings.get()["path"])
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printStateSource")
                .build()

        assertTrue("STATE_SOURCE=filesystem" in result.output, "Expected stateFile to set filesystem backend")
        assertTrue(
            "STATE_PATH=" + File(projectDir, "custom-state.yml").absolutePath in result.output,
            "Expected stateFile to set path setting",
        )
    }

    @Test
    fun `links defaults to empty when absent`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeSettings()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printLinks") {
                doLast {
                    println("EMPTY=" + terracotta.links.toModel().isEmpty())
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("printLinks")
                .build()

        assertTrue("EMPTY=true" in result.output, "Expected empty links by default")
    }
}
