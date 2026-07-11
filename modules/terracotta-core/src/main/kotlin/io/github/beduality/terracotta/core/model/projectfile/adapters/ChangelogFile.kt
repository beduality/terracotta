package io.github.beduality.terracotta.core.model.projectfile

import io.github.beduality.terracotta.core.model.projectfile.convention.ChangelogConvention
import io.github.beduality.terracotta.core.model.projectfile.convention.KeepAChangelogConvention

/**
 * Represents a `CHANGELOG.md` file in the project.
 *
 * Supports extracting the release notes for a specific version using a
 * configurable [ChangelogConvention]. The default is [KeepAChangelogConvention].
 */
class ChangelogFile(
    content: String?,
    private val convention: ChangelogConvention = KeepAChangelogConvention,
) : AbstractProjectFile(content) {
    companion object {
        private const val RELATIVE_PATH = "CHANGELOG.md"

        /**
         * Loads the changelog file from the given [cache] using the provided
         * [convention].
         */
        fun load(
            cache: ProjectFileCache,
            convention: ChangelogConvention = KeepAChangelogConvention,
        ): ChangelogFile = ChangelogFile(cache.read(RELATIVE_PATH), convention)
    }

    /**
     * Extracts the body of the release section for the given [version] using the
     * configured convention.
     *
     * Returns `null` if the section is not found.
     */
    fun extractVersionSection(version: String): String? {
        val currentContent = content ?: return null
        return convention.extractVersionSection(currentContent, version)
    }
}
