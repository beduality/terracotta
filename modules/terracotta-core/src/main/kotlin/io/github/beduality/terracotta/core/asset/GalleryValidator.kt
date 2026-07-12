package io.github.beduality.terracotta.core.asset

import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Validates a local gallery image before it is uploaded.
 *
 * Providers supply their supported [extensions] and [maxSizeBytes] limit; the core
 * validator checks that the file exists, has an allowed extension, and is not too large.
 */
object GalleryValidator {
    /**
     * Validates [imagePath] for upload.
     *
     * @throws IOException if the file is missing, has an unsupported extension, or
     *   exceeds [maxSizeBytes].
     */
    fun validate(
        imagePath: String,
        extensions: Set<String>,
        maxSizeBytes: Long,
    ) {
        val file = File(imagePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("Gallery image not found: $imagePath")
        }

        val extension = file.extension.lowercase(Locale.getDefault())
        if (extension !in extensions) {
            throw IOException(
                "Unsupported gallery image extension '$extension' for $imagePath. " +
                    "Supported extensions: ${extensions.joinToString(", ")}",
            )
        }

        if (file.length() > maxSizeBytes) {
            throw IOException(
                "Gallery image $imagePath exceeds maximum size of $maxSizeBytes bytes " +
                    "(actual: ${file.length()} bytes)",
            )
        }
    }
}
