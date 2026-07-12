package io.github.beduality.terracotta.core.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.name

class FileSystemStateSourceFactoryTest {
    @Test
    fun `factory id is filesystem`() {
        val factory = FileSystemStateSourceFactory()

        assertEquals("filesystem", factory.id)
    }

    @Test
    fun `resolves default path when no path setting is provided`(
        @TempDir projectDir: File,
    ) {
        val factory = FileSystemStateSourceFactory()
        val config = StateSourceConfig(projectDir = projectDir, settings = emptyMap())

        val source = factory.create(config)
        source.save(TerracottaState(projectId = "default-path"))

        assertEquals(
            TerracottaState(projectId = "default-path"),
            FileSystemStateSource.forDirectory(projectDir.toPath()).load(),
        )
    }

    @Test
    fun `resolves explicit path override`(
        @TempDir projectDir: File,
    ) {
        val factory = FileSystemStateSourceFactory()
        val config =
            StateSourceConfig(
                projectDir = projectDir,
                settings = mapOf("path" to File(projectDir, "custom-state.yml").absolutePath),
            )

        val source = factory.create(config)
        source.save(TerracottaState(projectId = "explicit-path"))

        assertEquals(
            TerracottaState(projectId = "explicit-path"),
            FileSystemStateSource.forFile(File(projectDir, "custom-state.yml").toPath()).load(),
        )
        assertEquals(0, projectDir.listFiles()!!.filter { it.name == ".terracotta-state.yml" }.size)
    }
}
