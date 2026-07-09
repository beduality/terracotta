package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaProject

object DiffEngine {
    /**
     * Computes the semantic diff between a desired local project state
     * and the actual remote project state.
     */
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
    ): List<Operation> {
        val operations = mutableListOf<Operation>()

        if (remote == null) {
            // If the project doesn't exist remotely, we configure it entirely
            operations.add(
                Operation.UpdateMetadata(
                    nameChanged = true,
                    summaryChanged = true,
                    licenseChanged = true,
                    newName = local.name,
                    newSummary = local.summary,
                    newLicense = local.license,
                ),
            )
            operations.add(Operation.UpdateDescription("", local.description))
            operations.add(Operation.UpdateTags(emptyList(), local.tags))
            local.versions.forEach { version ->
                operations.add(Operation.UploadVersion(version))
            }
            return operations
        }

        // Compare basic metadata
        val nameChanged = local.name != remote.name
        val summaryChanged = local.summary != remote.summary
        val licenseChanged = local.license.uppercase() != remote.license.uppercase()

        if (nameChanged || summaryChanged || licenseChanged) {
            operations.add(
                Operation.UpdateMetadata(
                    nameChanged = nameChanged,
                    summaryChanged = summaryChanged,
                    licenseChanged = licenseChanged,
                    newName = local.name,
                    newSummary = local.summary,
                    newLicense = local.license,
                ),
            )
        }

        // Compare description
        if (local.description != remote.description) {
            operations.add(Operation.UpdateDescription(remote.description, local.description))
        }

        // Compare tags
        val localTags = local.tags.toSet()
        val remoteTags = remote.tags.toSet()
        if (localTags != remoteTags) {
            operations.add(Operation.UpdateTags(remote.tags, local.tags))
        }

        // Compare versions (upload any missing local versions)
        val remoteVersionMap = remote.versions.associateBy { it.version }
        local.versions.forEach { localVersion ->
            val remoteVersion = remoteVersionMap[localVersion.version]
            if (remoteVersion == null) {
                operations.add(Operation.UploadVersion(localVersion))
            }
        }

        return operations
    }
}
