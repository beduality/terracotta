package io.github.beduality.terracotta.cli.smoke

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/**
 * Smoke tests for the Terracotta CLI.
 *
 * These tests exercise the CLI against the live Modrinth API and verify that
 * the end-to-end deployment workflow completes successfully.
 *
 * Prerequisites:
 *   - MODRINTH_TOKEN is set in the environment.
 *   - The CLI has been built with:
 *       ./gradlew :terracotta-cli:installDist
 *
 * Notes:
 *   - These tests interact with the live Modrinth API.
 *   - They create or update the configured test project.
 *   - They are intended for manual verification or CI smoke testing.
 *   - The suite is skipped automatically if the CLI binary has not been built.
 *   - Tagged "smoke" so they are excluded from the default `./gradlew test` run.
 *     Use `./gradlew smokeTest` to run them explicitly.
 */
@Tag("smoke")
class ApplySmokeTest {

    companion object {
        /** Root of the repository, resolved relative to the module's working directory. */
        private val ROOT: File = File("").absoluteFile.parentFile.parentFile

        private val CLI_BIN: File =
            ROOT.resolve("modules/terracotta-cli/build/install/terracotta-cli/bin/terracotta-cli")

        private const val PLUGIN_YML = """
name: TerracottaSmokeTest
version: 1.0.0
main: io.github.beduality.TerracottaSmokeTest
api-version: "1.20"
description: Dummy plugin for Terracotta smoke testing
author: Terracotta
"""
    }

    /**
     * Creates a minimal Paper plugin JAR in [dir] that satisfies the Modrinth upload requirements.
     */
    private fun createDummyJar(dir: File): File {
        val jar = File(dir, "my-plugin-1.0.0.jar")
        val manifest = Manifest().apply {
            mainAttributes["Manifest-Version"] = "1.0"
        }
        JarOutputStream(jar.outputStream(), manifest).use { jos ->
            jos.putNextEntry(JarEntry("plugin.yml"))
            jos.write(PLUGIN_YML.trimIndent().toByteArray())
            jos.closeEntry()
        }
        return jar
    }

    /**
     * Copies the bundled smoke-test config from test resources into [dir],
     * returning the written file.
     */
    private fun writeConfig(dir: File): File {
        val resource = ApplySmokeTest::class.java
            .getResourceAsStream("/smoke/terracotta-smoke-test.yaml")
            ?: error("Smoke test config resource not found")
        val config = File(dir, "terracotta-smoke-test.yaml")
        resource.use { config.writeBytes(it.readBytes()) }
        return config
    }

    @Test
    fun `apply command completes successfully against the live Modrinth API`(
        @TempDir tempDir: Path,
    ) {
        assumeTrue(
            CLI_BIN.exists(),
            "CLI binary not found at ${CLI_BIN.relativeTo(ROOT)}. " +
                "Run ./gradlew :terracotta-cli:installDist first.",
        )

        val dir = tempDir.toFile()
        createDummyJar(dir)
        val config = writeConfig(dir)

        // Write a minimal README so the config's `description: README.md` resolves
        File(dir, "README.md").writeText("# Terracotta Smoke Test")

        val process =
            ProcessBuilder(CLI_BIN.absolutePath, "apply", "-f", config.absolutePath)
                .directory(dir)
                .inheritIO()
                .start()

        val exitCode = process.waitFor()
        assertEquals(0, exitCode, "CLI apply command should exit with code 0")
    }
}
