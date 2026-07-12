package io.github.beduality.terracotta.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

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
}
