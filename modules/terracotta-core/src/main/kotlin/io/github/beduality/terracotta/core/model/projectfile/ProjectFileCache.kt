package io.github.beduality.terracotta.core.model.projectfile

import java.io.File

/**
 * Caches file reads for a project directory so multiple detectors can inspect
 * the same file without reading it from disk more than once.
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/conventions.html)
 */
class ProjectFileCache(private val projectDir: File) {
    private val cache = mutableMapOf<String, String?>()

    /**
     * Reads the file at the given [relativePath] under the project directory.
     *
     * Returns `null` if the file does not exist or is not a regular file. The
     * result is cached so subsequent reads of the same path return the same
     * value.
     *
     * @param relativePath path relative to the project directory.
     * @return file content, or `null` if the file does not exist.
     */
    fun read(relativePath: String): String? =
        cache.getOrPut(relativePath) {
            File(projectDir, relativePath)
                .takeIf { it.exists() && it.isFile }
                ?.readText()
        }
}
