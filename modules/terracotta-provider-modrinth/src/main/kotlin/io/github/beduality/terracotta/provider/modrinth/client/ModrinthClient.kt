package io.github.beduality.terracotta.provider.modrinth.client

import io.github.beduality.terracotta.core.asset.AssetProcessor
import io.github.beduality.terracotta.core.asset.GalleryValidator
import io.github.beduality.terracotta.core.asset.IdentityAssetProcessor
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaGalleryItem
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthProject
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * HTTP client for the Modrinth API.
 *
 * @see [Modrinth provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-modrinth/tutorials/using-modrinth.html)
 */
class ModrinthClient(
    /** Authentication token for Modrinth API requests. */
    private val token: String?,
    /** Base URL of the Modrinth API. */
    private val baseUrl: String = "https://api.modrinth.com/v2",
    /** Underlying Ktor HTTP client. */
    private val client: HttpClient = defaultClient(),
    /** Processor applied to asset files before upload. */
    private val assetProcessor: AssetProcessor = IdentityAssetProcessor,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    companion object {
        private val SUPPORTED_GALLERY_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
        private val SUPPORTED_ICON_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
        private const val MAX_GALLERY_SIZE_BYTES = 5L * 1024 * 1024
        private const val MAX_ICON_SIZE_BYTES = 256L * 1024

        private fun defaultClient(): HttpClient {
            return HttpClient(Java) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            encodeDefaults = false
                        },
                    )
                }
            }
        }
    }

    private val logger = LoggerFactory.getLogger(ModrinthClient::class.java)

    /** Fetches the Modrinth project with the given slug or ID. */
    suspend fun getProject(projectIdOrSlug: String): ModrinthProject? {
        val response =
            client.get("$baseUrl/project/$projectIdOrSlug") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
            }
        return when (response.status.value) {
            404 -> null
            !in 200..299 -> throw IOException("Failed to fetch project: ${response.status.value} ${response.bodyAsText()}")
            else -> response.body()
        }
    }

    /** Fetches all versions of the Modrinth project with the given slug or ID. */
    suspend fun getVersions(projectIdOrSlug: String): List<ModrinthVersion> {
        val response =
            client.get("$baseUrl/project/$projectIdOrSlug/version") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
            }
        return when (response.status.value) {
            404 -> emptyList()
            !in 200..299 -> throw IOException("Failed to fetch versions: ${response.status.value} ${response.bodyAsText()}")
            else -> response.body()
        }
    }

    /** Patches the Modrinth project with the given [patchData]. */
    suspend fun patchProject(
        projectIdOrSlug: String,
        patchData: Map<String, Any?>,
    ) {
        val body = patchData.toJsonObject()
        val response =
            client.patch("$baseUrl/project/$projectIdOrSlug") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to patch project: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully updated project metadata on Modrinth.")
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject =
        buildJsonObject {
            forEach { (key, value) -> putJsonElement(key, value) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun JsonArrayBuilder.addJsonElement(value: Any?) {
        when (value) {
            null -> add(JsonNull)
            is String -> add(value)
            is Number -> add(value)
            is Boolean -> add(value)
            is List<*> -> addJsonArray { value.forEach { addJsonElement(it) } }
            is Map<*, *> -> addJsonObject { (value as? Map<String, Any?>)?.forEach { (k, v) -> putJsonElement(k, v) } }
            else -> add(value.toString())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun JsonObjectBuilder.putJsonElement(
        key: String,
        value: Any?,
    ) {
        when (value) {
            null -> put(key, JsonNull)
            is String -> put(key, value)
            is Number -> put(key, value)
            is Boolean -> put(key, value)
            is List<*> -> put(key, buildJsonArray { value.forEach { addJsonElement(it) } })
            is Map<*, *> -> put(key, buildJsonObject { (value as? Map<String, Any?>)?.forEach { (k, v) -> putJsonElement(k, v) } })
            else -> put(key, value.toString())
        }
    }

    /** Uploads [version] to the Modrinth project identified by [projectId]. */
    suspend fun createVersion(
        projectId: String,
        version: TerracottaVersion,
    ) {
        val artifactFile = File(version.artifactPath)
        if (!artifactFile.exists()) {
            throw IOException("Artifact file not found: ${version.artifactPath}")
        }

        val dataPart: JsonObject =
            buildJsonObject {
                put("name", version.displayName)
                put("version_number", version.version)
                put("game_versions", buildJsonArray { version.gameVersions.forEach { add(it) } })
                put("loaders", buildJsonArray { version.loaders.forEach { add(it) } })
                put("project_id", projectId)
                put("file_parts", buildJsonArray { add("file_0") })
                put("changelog", version.changelog)
                put("dependencies", JsonArray(emptyList()))
                put("version_type", version.releaseType.id)
                put("featured", false)
            }

        val response =
            client.post("$baseUrl/version") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("data", json.encodeToString(JsonObject.serializer(), dataPart))
                            append(
                                "file_0",
                                artifactFile.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${artifactFile.name}\"")
                                    append(HttpHeaders.ContentType, "application/java-archive")
                                },
                            )
                        },
                    ),
                )
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to upload version: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully uploaded version ${version.version} to Modrinth.")
    }

    /** Deletes the Modrinth project identified by [projectId]. */
    suspend fun deleteProject(projectId: String) {
        val response =
            client.delete("$baseUrl/project/$projectId") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
            }
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw IOException("Failed to delete project: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully deleted project $projectId on Modrinth.")
    }

    /** Deletes the Modrinth version identified by [versionId]. */
    suspend fun deleteVersion(versionId: String) {
        val response =
            client.delete("$baseUrl/version/$versionId") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
            }
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw IOException("Failed to delete version: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully deleted version $versionId on Modrinth.")
    }

    /** Creates a new Modrinth draft project and returns its generated ID. */
    suspend fun createProject(project: TerracottaProject): String {
        val environment = project.versions.firstOrNull()?.environment ?: TerracottaEnvironment.SERVER_ONLY
        val dataPart: JsonObject =
            buildJsonObject {
                put("slug", project.id)
                put("title", project.name)
                put("description", project.summary)
                put("body", project.description)
                put("categories", buildJsonArray { project.tags.forEach { add(it) } })
                put("client_side", environment.toModrinthClientSide())
                put("server_side", environment.toModrinthServerSide())
                put("project_type", "mod")
                put("license_id", project.license)
                project.licenseUrl?.let { put("license_url", it) }
                project.links.issues?.let { put("issues_url", it) }
                project.links.source?.let { put("source_url", it) }
                project.links.wiki?.let { put("wiki_url", it) }
                project.links.community?.let { put("discord_url", it) }
                if (project.links.donations.isNotEmpty()) {
                    put(
                        "donation_urls",
                        buildJsonArray {
                            project.links.donations.forEach { donation ->
                                addJsonObject {
                                    put("id", donation.platform)
                                    put("platform", donation.platform)
                                    put("url", donation.url)
                                }
                            }
                        },
                    )
                }
                put("is_draft", true)
                // initial_versions is deprecated but the API still requires the field to be present
                put("initial_versions", JsonArray(emptyList()))
            }

        val response =
            client.post("$baseUrl/project") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("data", json.encodeToString(JsonObject.serializer(), dataPart))
                        },
                    ),
                )
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to create project: ${response.status.value} ${response.bodyAsText()}")
        }
        val createdProject: ModrinthProject = response.body()
        logger.info("Successfully created project ${project.name} on Modrinth with ID ${createdProject.id}.")
        return createdProject.id
    }

    private fun TerracottaEnvironment.toModrinthClientSide(): String =
        when (this) {
            TerracottaEnvironment.CLIENT_ONLY -> "required"
            TerracottaEnvironment.SERVER_ONLY -> "optional"
            TerracottaEnvironment.UNIVERSAL -> "required"
        }

    private fun TerracottaEnvironment.toModrinthServerSide(): String =
        when (this) {
            TerracottaEnvironment.CLIENT_ONLY -> "unsupported"
            TerracottaEnvironment.SERVER_ONLY -> "required"
            TerracottaEnvironment.UNIVERSAL -> "required"
        }

    /** Uploads [item] to the gallery of the project identified by [projectId]. */
    suspend fun uploadGalleryItem(
        projectId: String,
        item: TerracottaGalleryItem,
    ) {
        val processed = assetProcessor.process(File(item.imagePath))
        GalleryValidator.validate(
            processed.path,
            SUPPORTED_GALLERY_EXTENSIONS,
            MAX_GALLERY_SIZE_BYTES,
        )

        val extension = processed.extension.ifBlank { File(item.imagePath).extension }
        val file = File(processed.path)

        val response =
            client.post("$baseUrl/project/$projectId/gallery") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                parameter("ext", extension)
                parameter("featured", item.featured)
                parameter("title", item.title)
                parameter("description", item.description)
                parameter("ordering", item.ordering)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                    append(HttpHeaders.ContentType, processed.contentType)
                                },
                            )
                        },
                    ),
                )
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to upload gallery image: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully uploaded gallery image '${item.title}' to Modrinth project $projectId.")
    }

    /** Updates the gallery image identified by [url] on the project [projectId]. */
    suspend fun updateGalleryItem(
        projectId: String,
        url: String,
        item: TerracottaGalleryItem,
    ) {
        val response =
            client.patch("$baseUrl/project/$projectId/gallery") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                parameter("url", url)
                parameter("title", item.title)
                parameter("description", item.description)
                parameter("featured", item.featured)
                parameter("ordering", item.ordering)
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to update gallery image: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully updated gallery image '${item.title}' on Modrinth project $projectId.")
    }

    /** Deletes the gallery image identified by [url] from the project [projectId]. */
    suspend fun deleteGalleryItem(
        projectId: String,
        url: String,
    ) {
        val response =
            client.delete("$baseUrl/project/$projectId/gallery") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                parameter("url", url)
            }
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw IOException("Failed to delete gallery image: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully deleted gallery image from Modrinth project $projectId.")
    }

    /** Uploads [iconPath] as the icon of the project identified by [projectId]. */
    suspend fun uploadIcon(
        projectId: String,
        iconPath: String,
    ) {
        val processed = assetProcessor.process(File(iconPath))
        GalleryValidator.validate(
            processed.path,
            SUPPORTED_ICON_EXTENSIONS,
            MAX_ICON_SIZE_BYTES,
        )

        val extension = processed.extension.ifBlank { File(iconPath).extension }
        val file = File(processed.path)

        val response =
            client.patch("$baseUrl/project/$projectId/icon") {
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, token)
                }
                parameter("ext", extension)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "icon",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                    append(HttpHeaders.ContentType, processed.contentType)
                                },
                            )
                        },
                    ),
                )
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to upload project icon: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully uploaded project icon to Modrinth project $projectId.")
    }
}
