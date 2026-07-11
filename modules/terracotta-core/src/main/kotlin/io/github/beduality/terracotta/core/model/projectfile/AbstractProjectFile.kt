package io.github.beduality.terracotta.core.model.projectfile

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

/**
 * Base class for a logical project file that may be loaded from the project
 * directory.
 *
 * A logical file may have several physical candidate paths (for example
 * `LICENSE`, `LICENSE.txt`, or `LICENSE.md`). Subclasses define those paths,
 * load themselves from a [ProjectFileCache], and expose file-specific derived
 * properties that detectors can read.
 */
abstract class AbstractProjectFile(
    /** Raw file content, or null if the file does not exist. */
    val content: String?,
) {
    /** Returns `true` when the file was found and has content. */
    val exists: Boolean get() = content != null
}
