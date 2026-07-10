package io.github.beduality.terracotta.core.diff

import io.github.beduality.terracotta.core.model.TerracottaVersion

object OperationPreprocessor {
    private const val DEFAULT_CHANGELOG = "Uploaded via Terracotta."

    fun process(operations: List<Operation>): List<Operation> {
        return operations.map { operation ->
            when (operation) {
                is Operation.UploadVersion ->
                    operation.copy(
                        version = normalizeVersion(operation.version),
                    )
                is Operation.CreateProject ->
                    operation.copy(
                        project =
                            operation.project.copy(
                                versions = operation.project.versions.map { normalizeVersion(it) },
                            ),
                    )
                else -> operation
            }
        }
    }

    private fun normalizeVersion(version: TerracottaVersion): TerracottaVersion {
        return version.copy(
            changelog = version.changelog.ifEmpty { DEFAULT_CHANGELOG },
            displayName = "Version ${version.version}",
        )
    }
}
