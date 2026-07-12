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

        assertTrue(exception.message!!.contains("unknown"), "Expected message to mention unknown backend id")
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
