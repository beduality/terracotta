package io.github.beduality.terracotta.core.asset

import java.io.File

/**
 * Transforms a local asset file before it is uploaded to a provider.
 *
 * Implementations may compress, convert, or otherwise prepare the file. The default
 * implementation returns the original file unchanged.
 *
 * @see [Asset processing explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/asset-processing.html)
 */
interface AssetProcessor {
    /**
     * Processes [inputFile] and returns a file ready for upload.
     *
     * @return a [ProcessedAsset] describing the file to upload.
     */
    fun process(inputFile: File): ProcessedAsset
}

/**
 * Describes an asset file after processing.
 *
 * @property path Path to the processed file.
 * @property contentType MIME type of the processed file.
 * @property extension File extension to use when uploading.
 */
data class ProcessedAsset(
    /** Path to the processed file. */
    val path: String,
    /** MIME type of the processed file. */
    val contentType: String,
    /** File extension to use when uploading. */
    val extension: String,
)

/** No-op [AssetProcessor] that returns the original file. */
object IdentityAssetProcessor : AssetProcessor {
    override fun process(inputFile: File): ProcessedAsset {
        return ProcessedAsset(
            path = inputFile.absolutePath,
            contentType = "application/octet-stream",
            extension = inputFile.extension,
        )
    }
}
