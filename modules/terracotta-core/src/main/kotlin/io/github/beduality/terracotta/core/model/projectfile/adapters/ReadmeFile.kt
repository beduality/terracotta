package io.github.beduality.terracotta.core.model.projectfile

import io.github.beduality.terracotta.core.model.projectfile.convention.ReadmeConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.TerracottaReadmeConvention

/**
 * Represents a `README.md` file in the project.
 *
 * Exposes the full description and a short summary using a configurable
 * [ReadmeConvention]. The default is [TerracottaReadmeConvention].
 *
 * @see [Conventions reference](https://beduality.github.io/terracotta/content/modules/core/reference/conventions.html)
 * @see [Add a project-file convention guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-project-file-convention.html)
 */
class ReadmeFile(
    content: String?,
    private val convention: ReadmeConvention = TerracottaReadmeConvention,
) : AbstractProjectFile(content) {
    companion object {
        private const val RELATIVE_PATH = "README.md"

        /**
         * Loads the README file from the given [cache] using the provided
         * [convention].
         */
        fun load(
            cache: ProjectFileCache,
            convention: ReadmeConvention = TerracottaReadmeConvention,
        ): ReadmeFile = ReadmeFile(cache.read(RELATIVE_PATH), convention)
    }

    /** Full description, or null if the README is missing or empty. */
    val description: String? =
        content?.let { convention.extractDescription(it) }

    /** First non-heading paragraph, or null if none is found. */
    val summary: String? =
        content?.let { convention.extractSummary(it) }
}
