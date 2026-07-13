package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
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

    /** Updates the project categories from [oldCategories] to [newCategories]. */
    data class UpdateCategories(
        val oldCategories: TerracottaProjectCategories,
        val newCategories: TerracottaProjectCategories,
    ) : Operation {
        override val description: String =
            "~ Update categories (from: ${formatCategories(oldCategories)} to: ${formatCategories(newCategories)})"

        private fun formatCategories(categories: TerracottaProjectCategories): String =
            buildString {
                append(categories.primary.id)
                if (categories.additional.isNotEmpty()) {
                    append(", ")
                    append(categories.additional.joinToString { it.id })
                }
            }
    }

    /**
     * Updates project metadata fields that changed.
     *
     * @property nameChanged whether the project name changed.
     * @property summaryChanged whether the project summary changed.
     * @property licenseChanged whether the project license changed.
     * @property licenseUrlChanged whether the project license URL changed.
     * @property linksChanged whether the project links changed.
     * @property newName the new project name.
     * @property newSummary the new project summary.
     * @property newLicense the new SPDX license identifier.
     * @property newLicenseUrl the new optional URL to the full license text.
     * @property newLinks the new canonical project links.
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
        /** Whether the project links changed. */
        val linksChanged: Boolean,
        /** New project name. */
        val newName: String,
        /** New project summary. */
        val newSummary: String,
        /** New SPDX license identifier. */
        val newLicense: String,
        /** New optional URL to the full license text. */
        val newLicenseUrl: String?,
        /** New canonical project links. */
        val newLinks: TerracottaProjectLinks,
    ) : Operation {
        override val description: String
            get() {
                val changes = mutableListOf<String>()
                if (nameChanged) changes.add("name")
                if (summaryChanged) changes.add("summary")
                if (licenseChanged) changes.add("license")
                if (licenseUrlChanged) changes.add("licenseUrl")
                if (linksChanged) changes.add("links")
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

    /** Uploads [iconPath] as the project icon. */
    data class UploadIcon(val iconPath: String) : Operation {
        override val description: String = "+ Upload project icon"
    }

    /**
     * Replaces the existing project icon with [iconPath].
     *
     * @property oldIconUrl Previous icon URL on the remote project, or null if unknown.
     * @property iconPath Local path to the new icon file.
     */
    data class UpdateIcon(
        /** Previous icon URL on the remote project, or null if unknown. */
        val oldIconUrl: String?,
        /** Local path to the new icon file. */
        val iconPath: String,
    ) : Operation {
        override val description: String = "~ Update project icon"
    }

    /** Deletes the project icon identified by [iconUrl] from the remote project. */
    data class DeleteIcon(val iconUrl: String) : Operation {
        override val description: String = "- Delete project icon"
    }
}
