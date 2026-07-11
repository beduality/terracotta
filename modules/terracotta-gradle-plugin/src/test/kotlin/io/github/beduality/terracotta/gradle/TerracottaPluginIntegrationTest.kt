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
}
