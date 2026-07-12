package io.github.beduality.terracotta.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private fun File.writeSettings(projectName: String = "integration-test") {
    File(this, "settings.gradle.kts").writeText(
        """
        rootProject.name = "$projectName"
        """.trimIndent(),
    )
}

private fun File.writeTerracottaYml(
    name: String? = null,
    providers: Map<String, Map<String, String?>> = emptyMap(),
) {
    val content =
        buildString {
            name?.let { appendLine("name: \"$it\"") }
            if (providers.isNotEmpty()) {
                appendLine("providers:")
                providers.forEach { (providerId, config) ->
                    appendLine("  $providerId:")
                    config.forEach { (key, value) ->
                        if (value != null) appendLine("    $key: $value")
                    }
                }
            }
        }
    File(this, "terracotta.yml").writeText(content.trimIndent())
}

private fun File.writeMinimalProject(
    projectName: String = "integration-test",
    version: String? = null,
    providers: Map<String, Map<String, String?>> = emptyMap(),
) {
    writeSettings(projectName)
    writeTerracottaYml(providers = providers)
    version?.let { File(this, "gradle.properties").writeText("version=$it") }
    File(this, "build.gradle.kts").writeText(
        """
        plugins {
            id("io.github.beduality.terracotta")
        }
        """.trimIndent(),
    )
}

private fun runGradle(
    projectDir: File,
    vararg args: String,
    env: Map<String, String> = emptyMap(),
): BuildResult {
    return GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath(pluginClasspathWithFileSystem())
        .withEnvironment(env)
        .withArguments(*args)
        .build()
}

class TerracottaPluginTaskIntegrationTest {
    @Test
    fun `registers aggregate terracottaPlan and terracottaApply tasks`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers = mapOf("modrinth" to mapOf("projectId" to "my-plugin")),
        )

        val result = runGradle(projectDir, "tasks", "--all")

        assertTrue("terracottaPlan" in result.output, "Expected aggregate terracottaPlan task")
        assertTrue("terracottaApply" in result.output, "Expected aggregate terracottaApply task")
    }

    @Test
    fun `registers provider specific tasks for DSL created providers`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                providers.create("modrinth") {
                    projectId.set("dsl-plugin")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDir, "tasks", "--all")

        assertTrue("terracottaPlanModrinth" in result.output, "Expected terracottaPlanModrinth task")
        assertTrue("terracottaApplyModrinth" in result.output, "Expected terracottaApplyModrinth task")
    }

    @Test
    fun `yaml provider token takes precedence over environment variable`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers =
                mapOf(
                    "modrinth" to
                        mapOf(
                            "projectId" to "my-plugin",
                            "token" to "yaml-token",
                        ),
                ),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printToken") {
                doLast {
                    println("TOKEN=" + terracotta.providers["modrinth"].token.orNull)
                }
            }
            """.trimIndent(),
        )

        val result =
            runGradle(
                projectDir,
                "printToken",
                env = mapOf("MODRINTH_TOKEN" to "env-token"),
            )

        assertTrue("TOKEN=yaml-token" in result.output, "Expected YAML token to win over env var")
    }

    @Test
    fun `falls back to environment variable token when yaml omits token`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers = mapOf("modrinth" to mapOf("projectId" to "my-plugin")),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            tasks.register("printToken") {
                doLast {
                    println("TOKEN=" + terracotta.providers["modrinth"].token.orNull)
                }
            }
            """.trimIndent(),
        )

        val result =
            runGradle(
                projectDir,
                "printToken",
                env = mapOf("MODRINTH_TOKEN" to "env-token"),
            )

        assertTrue("TOKEN=env-token" in result.output, "Expected env var fallback for token")
    }

    @Test
    fun `DSL provider projectId overrides yaml projectId`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers =
                mapOf(
                    "modrinth" to
                        mapOf(
                            "projectId" to "yaml-plugin",
                            "token" to "yaml-token",
                        ),
                ),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                providers.create("modrinth") {
                    projectId.set("dsl-plugin")
                }
            }

            tasks.register("printProjectId") {
                doLast {
                    println("PROJECTID=" + terracotta.providers["modrinth"].projectId.orNull)
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDir, "printProjectId")

        assertTrue("PROJECTID=dsl-plugin" in result.output, "Expected DSL value to override YAML")
    }

    @Test
    fun `artifact file convention wires to jar task when java plugin is applied`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(version = "1.2.3")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("io.github.beduality.terracotta")
            }

            tasks.register("printArtifact") {
                doLast {
                    println("ARTIFACT=" + terracotta.artifactFile.get().asFile.path.replace("\\", "/"))
                }
            }
            """.trimIndent(),
        )
        File(projectDir, "src/main/java/Dummy.java").apply { parentFile.mkdirs() }.writeText("class Dummy {}")

        val result = runGradle(projectDir, "printArtifact")

        val expectedSuffix = "build/libs/integration-test-1.2.3.jar"
        val artifactLine = result.output.lines().find { it.startsWith("ARTIFACT=") }
        assertTrue(
            artifactLine?.replace("\\", "/")?.endsWith(expectedSuffix) == true,
            "Expected artifact file to default to jar output: $artifactLine",
        )
    }

    @Test
    fun `fails with clear message when provider implementation is missing`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers = mapOf("modrinth" to mapOf("projectId" to "my-plugin")),
        )
        File(projectDir, "build/dummy.jar").apply {
            parentFile.mkdirs()
            createNewFile()
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.beduality.terracotta")
            }

            terracotta {
                artifactFile.set(layout.projectDirectory.file("build/dummy.jar"))
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath(pluginClasspathWithFileSystem())
                .withArguments("terracottaPlanModrinth")
                .buildAndFail()

        assertTrue(
            "No provider found with id 'modrinth'" in result.output,
            "Expected clear missing-provider error message",
        )
    }

    @Test
    fun `terracottaApplyModrinth depends on terracottaPlanModrinth`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers = mapOf("modrinth" to mapOf("projectId" to "my-plugin")),
        )

        val result = runGradle(projectDir, "terracottaApplyModrinth", "--dry-run")

        assertTrue("terracottaPlanModrinth" in result.output, "Expected plan task in apply graph")
        assertTrue("terracottaApplyModrinth" in result.output, "Expected apply task in output")
    }

    @Test
    fun `registers aggregate terracottaDestroy task and per-provider destroy tasks`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers =
                mapOf(
                    "modrinth" to mapOf("projectId" to "my-plugin"),
                    "hangar" to mapOf("projectId" to "my-plugin"),
                ),
        )

        val result = runGradle(projectDir, "tasks", "--all")

        assertTrue("terracottaDestroy" in result.output, "Expected aggregate terracottaDestroy task")
        assertTrue("terracottaDestroyModrinth" in result.output, "Expected terracottaDestroyModrinth task")
        assertTrue("terracottaDestroyHangar" in result.output, "Expected terracottaDestroyHangar task")
    }

    @Test
    fun `terracottaDestroy depends on all provider destroy tasks`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers =
                mapOf(
                    "modrinth" to mapOf("projectId" to "my-plugin"),
                    "hangar" to mapOf("projectId" to "my-plugin"),
                ),
        )

        val result = runGradle(projectDir, "terracottaDestroy", "--dry-run")

        assertTrue("terracottaDestroyModrinth" in result.output, "Expected modrinth destroy task in aggregate graph")
        assertTrue("terracottaDestroyHangar" in result.output, "Expected hangar destroy task in aggregate graph")
    }

    @Test
    fun `terracottaPlan depends on all provider plan tasks`(
        @TempDir projectDir: File,
    ) {
        projectDir.writeMinimalProject(
            providers =
                mapOf(
                    "modrinth" to mapOf("projectId" to "my-plugin"),
                    "hangar" to mapOf("projectId" to "my-plugin"),
                ),
        )

        val result = runGradle(projectDir, "terracottaPlan", "--dry-run")

        assertTrue("terracottaPlanModrinth" in result.output, "Expected modrinth plan task in aggregate graph")
        assertTrue("terracottaPlanHangar" in result.output, "Expected hangar plan task in aggregate graph")
    }
}
