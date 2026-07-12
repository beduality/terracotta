package io.github.beduality.terracotta.core.state

import java.io.File
import java.nio.file.Path

/**
 * [StateSourceFactory] for the file-backed state backend.
 *
 * Reads the `path` setting from [StateSourceConfig.settings] and falls back to
 * `.terracotta-state.yml` in [StateSourceConfig.projectDir].
 *
 * @see [State Management](https://beduality.github.io/terracotta/content/modules/core/explanation/state-management.html)
 * @see [State Filesystem Reference](https://beduality.github.io/terracotta/content/modules/terracotta-state-filesystem/reference/state-filesystem.html)
 */
class FileSystemStateSourceFactory : StateSourceFactory {
    override val id: String = "filesystem"

    override fun create(config: StateSourceConfig): StateSource {
        val path: Path =
            config.settings["path"]
                ?.let { File(it).toPath() }
                ?: config.projectDir.toPath().resolve(FileSystemStateSource.DEFAULT_FILE_NAME)
        return FileSystemStateSource.forFile(path)
    }
}
