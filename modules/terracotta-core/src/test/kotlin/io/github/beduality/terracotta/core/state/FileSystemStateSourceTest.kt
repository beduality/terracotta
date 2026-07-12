package io.github.beduality.terracotta.core.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant

class FileSystemStateSourceTest {
    @Test
    fun `returns empty state when file does not exist`(
        @TempDir tempDir: File,
    ) {
        val source = FileSystemStateSource.forDirectory(tempDir.toPath())

        val state = source.load()

        assertEquals(1, state.version)
        assertNull(state.lastRun)
        assertNull(state.projectId)
        assertTrue(state.providers.isEmpty())
    }

    @Test
    fun `round trips full state through yaml file`(
        @TempDir tempDir: File,
    ) {
        val source = FileSystemStateSource.forDirectory(tempDir.toPath())
        val startedAt = Instant.parse("2026-07-12T03:00:00Z")
        val finishedAt = Instant.parse("2026-07-12T03:00:42Z")
        val state =
            TerracottaState(
                lastRun =
                    RunSummary(
                        command = "apply",
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                        commitSha = "a1b2c3d",
                    ),
                projectId = "my-awesome-mod",
                providers =
                    mapOf(
                        "modrinth" to
                            ProviderState(
                                versionIds = listOf("1.0.0", "1.0.1"),
                                gallery =
                                    mapOf(
                                        "mainScreenshot" to
                                            GalleryItemIdentity(
                                                localKey = "mainScreenshot",
                                                remoteUrl = "https://cdn.modrinth.com/...",
                                                remoteId = "abc123",
                                            ),
                                    ),
                                metadataHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                            ),
                    ),
            )

        source.save(state)
        val loaded = source.load()

        assertEquals(state, loaded)
    }

    @Test
    fun `overwrites existing state`(
        @TempDir tempDir: File,
    ) {
        val source = FileSystemStateSource.forDirectory(tempDir.toPath())
        source.save(TerracottaState(projectId = "old"))

        source.save(TerracottaState(projectId = "new"))
        val loaded = source.load()

        assertEquals("new", loaded.projectId)
    }

    @Test
    fun `throws descriptive io exception when state file is corrupt`(
        @TempDir tempDir: File,
    ) {
        val stateFile = File(tempDir, ".terracotta-state.yml")
        stateFile.writeText("not: valid: [yaml")
        val source = FileSystemStateSource.forDirectory(tempDir.toPath())

        val exception =
            assertThrows<IOException> {
                source.load()
            }

        assertTrue(exception.message!!.contains(stateFile.name))
    }

    @Test
    fun `does not leave temporary file behind after save`(
        @TempDir tempDir: File,
    ) {
        val source = FileSystemStateSource.forDirectory(tempDir.toPath())

        source.save(TerracottaState(projectId = "clean"))

        assertEquals(1, tempDir.listFiles()!!.size)
        assertTrue(File(tempDir, ".terracotta-state.yml").exists())
    }
}
