package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.version.TerracottaVersion

/**
 * @see [Operations reference](https://beduality.github.io/terracotta/content/modules/core/reference/operations.html)
 */
sealed interface Operation {
    /** Human-readable description of this operation. */
    val description: String

    /** Updates the project description from [oldDescription] to [newDescription]. */
    data class UpdateDescription(val oldDescription: String, val newDescription: String) : Operation {
        override val description: String = "~ Update description"
    }

    /** Updates the project tags from [oldTags] to [newTags]. */
    data class UpdateTags(val oldTags: List<String>, val newTags: List<String>) : Operation {
        override val description: String = "~ Update tags (from: ${oldTags.joinToString()} to: ${newTags.joinToString()})"
    }

    /**
     * Updates project metadata fields that changed.
     *
     * @property nameChanged whether the project name changed.
     * @property summaryChanged whether the project summary changed.
     * @property licenseChanged whether the project license changed.
     * @property licenseUrlChanged whether the project license URL changed.
     * @property newName the new project name.
     * @property newSummary the new project summary.
     * @property newLicense the new SPDX license identifier.
     * @property newLicenseUrl the new optional URL to the full license text.
     */
    data class UpdateMetadata(
        /** Whether the project name changed. */
        val nameChanged: Boolean,
        /** Whether the project summary changed. */
        val summaryChanged: Boolean,
        /** Whether the project license changed. */
        val licenseChanged: Boolean,
        /** Whether the project license URL changed. */
        val licenseUrlChanged: Boolean,
        /** New project name. */
        val newName: String,
        /** New project summary. */
        val newSummary: String,
        /** New SPDX license identifier. */
        val newLicense: String,
        /** New optional URL to the full license text. */
        val newLicenseUrl: String?,
    ) : Operation {
        override val description: String
            get() {
                val changes = mutableListOf<String>()
                if (nameChanged) changes.add("name")
                if (summaryChanged) changes.add("summary")
                if (licenseChanged) changes.add("license")
                if (licenseUrlChanged) changes.add("licenseUrl")
                return "~ Update project metadata (${changes.joinToString(", ")})"
            }
    }

    /** Uploads [version] as a new release. */
    data class UploadVersion(val version: TerracottaVersion) : Operation {
        override val description: String = "+ Upload version ${version.version}"
    }

    /** Creates the remote project described by [project]. */
    data class CreateProject(val project: io.github.beduality.terracotta.core.model.TerracottaProject) : Operation {
        override val description: String = "+ Create project ${project.name}"
    }

    /** Uploads [item] as a new gallery image. */
    data class UploadGalleryItem(val item: TerracottaGalleryItem) : Operation {
        override val description: String = "+ Upload gallery image '${item.title}'"
    }

    /**
     * Updates an existing gallery image from [oldItem] to [newItem].
     *
     * Providers that cannot update in place may implement this as a delete followed
     * by an upload.
     */
    data class UpdateGalleryItem(
        /** Previous state of the gallery item. */
        val oldItem: TerracottaGalleryItem,
        /** Desired new state of the gallery item. */
        val newItem: TerracottaGalleryItem,
    ) : Operation {
        override val description: String = "~ Update gallery image '${newItem.title}'"
    }

    /** Deletes the gallery image described by [item] from the remote project. */
    data class DeleteGalleryItem(val item: TerracottaGalleryItem) : Operation {
        override val description: String = "- Delete gallery image '${item.title}'"
    }
}
