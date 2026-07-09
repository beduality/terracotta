package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaVersion

sealed interface Operation {
    val description: String

    data class UpdateDescription(val oldDescription: String, val newDescription: String) : Operation {
        override val description: String = "~ Update description"
    }

    data class UpdateTags(val oldTags: List<String>, val newTags: List<String>) : Operation {
        override val description: String = "~ Update tags (from: ${oldTags.joinToString()} to: ${newTags.joinToString()})"
    }

    data class UpdateMetadata(
        val nameChanged: Boolean,
        val summaryChanged: Boolean,
        val licenseChanged: Boolean,
        val newName: String,
        val newSummary: String,
        val newLicense: String,
    ) : Operation {
        override val description: String
            get() {
                val changes = mutableListOf<String>()
                if (nameChanged) changes.add("name")
                if (summaryChanged) changes.add("summary")
                if (licenseChanged) changes.add("license")
                return "~ Update project metadata (${changes.joinToString(", ")})"
            }
    }

    data class UploadVersion(val version: TerracottaVersion) : Operation {
        override val description: String = "+ Upload version ${version.version}"
    }
}
