package io.github.beduality.terracotta.core.state

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * [StateSource] backed by a YAML file on the local filesystem.
 *
 * The default filename is `.terracotta-state.yml`. It is written atomically: a
 * temporary file is created next to the target, then moved into place.
 *
 * @see [State Management](https://beduality.github.io/terracotta/content/modules/core/explanation/state-management.html)
 */
class FileSystemStateSource private constructor(
    private val file: Path,
) : StateSource {
    override fun load(): TerracottaState {
        if (!file.exists()) return TerracottaState()
        return try {
            val yaml = file.readText()
            YamlStateCodec.decode(yaml)
        } catch (e: IOException) {
            throw IOException("Failed to load state file $file: ${e.message}", e)
        }
    }

    override fun save(state: TerracottaState) {
        try {
            val yaml = YamlStateCodec.encode(state)
            val temp = file.resolveSibling(".${file.fileName}.tmp")
            temp.writeText(yaml)
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw IOException("Failed to save state file $file: ${e.message}", e)
        }
    }

    companion object {
        const val DEFAULT_FILE_NAME = ".terracotta-state.yml"

        fun forFile(file: Path): FileSystemStateSource = FileSystemStateSource(file)

        fun forDirectory(directory: Path): FileSystemStateSource = FileSystemStateSource(directory.resolve(DEFAULT_FILE_NAME))
    }
}
