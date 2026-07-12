package io.github.beduality.terracotta.gradle

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StateSourceResolverTest {
    @Test
    fun `resolves filesystem backend by default`(
        @TempDir projectDir: File,
    ) {
        val source = StateSourceResolver.resolve("filesystem", projectDir, emptyMap())

        source.save(io.github.beduality.terracotta.core.state.TerracottaState(projectId = "resolved"))

        assertTrue(File(projectDir, ".terracotta-state.yml").exists())
    }

    @Test
    fun `fails with clear message when backend is unknown`(
        @TempDir projectDir: File,
    ) {
        val exception =
            assertThrows<GradleException> {
                StateSourceResolver.resolve("unknown", projectDir, emptyMap())
            }

        val message = exception.message!!
        assertTrue(
            "No state source factory found with id 'unknown'" in message,
            "Expected message to mention unknown backend id",
        )
        assertTrue(
            "Available factories:" in message,
            "Expected message to list available factories",
        )
        assertTrue(
            "filesystem" in message,
            "Expected filesystem factory to be listed as available",
        )
        assertTrue(
            "Make sure the backend module is on the classpath." in message,
            "Expected message to advise checking the classpath",
        )
        assertTrue(
            "terracotta-state-filesystem" !in message,
            "Expected no filesystem-specific hint for an arbitrary unknown backend",
        )
    }

    @Test
    fun `passes settings to factory`(
        @TempDir projectDir: File,
    ) {
        val customFile = File(projectDir, "custom-state.yml")
        val source =
            StateSourceResolver.resolve(
                "filesystem",
                projectDir,
                mapOf("path" to customFile.absolutePath),
            )

        source.save(io.github.beduality.terracotta.core.state.TerracottaState(projectId = "custom"))

        assertTrue(customFile.exists())
        assertEquals(0, projectDir.listFiles()!!.filter { it.name == ".terracotta-state.yml" }.size)
    }
}
