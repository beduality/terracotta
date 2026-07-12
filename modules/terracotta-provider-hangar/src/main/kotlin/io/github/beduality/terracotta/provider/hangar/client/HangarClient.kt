package io.github.beduality.terracotta.provider.hangar.client

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProjectLinks
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.provider.logic.LoaderMapper
import io.github.beduality.terracotta.provider.hangar.mapper.HangarLoaderMapper
import io.github.beduality.terracotta.provider.hangar.model.HangarAuthenticateResponse
import io.github.beduality.terracotta.provider.hangar.model.HangarChannel
import io.github.beduality.terracotta.provider.hangar.model.HangarProject
import io.github.beduality.terracotta.provider.hangar.model.HangarVersion
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * HTTP client for the Hangar API.
 *
 * Handles JWT authentication, project metadata updates, version uploads, and
 * channel creation.
 *
 * @see [Hangar provider tutorial](https://beduality.github.io/terracotta/content/modules/provider-hangar/tutorials/using-hangar.html)
 */
class HangarClient(
    /** Hangar API key. */
    private val apiKey: String?,
    /** Base URL of the Hangar API. */
    private val baseUrl: String = "https://hangar.papermc.io/api/v1",
    /** Loader mapper used to translate Terracotta loaders to Hangar platforms. */
    private val loaderMapper: LoaderMapper = HangarLoaderMapper,
    /** Underlying Ktor HTTP client. */
    private val client: HttpClient = defaultClient(),
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private val logger = LoggerFactory.getLogger(HangarClient::class.java)

    private var jwt: String? = null
    private var expiresAt: Long = 0

    private suspend fun token(): String? {
        if (apiKey.isNullOrBlank()) return null
        if (jwt == null || System.currentTimeMillis() >= expiresAt - REFRESH_MARGIN) {
            refreshToken()
        }
        return jwt
    }

    private suspend fun refreshToken() {
        val response =
            client.post("$baseUrl/authenticate") {
                parameter("apiKey", apiKey)
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to authenticate with Hangar: ${response.status.value} ${response.bodyAsText()}")
        }
        val auth: HangarAuthenticateResponse = response.body()
        jwt = auth.token
        expiresAt = auth.expiresAt?.let { it - REFRESH_MARGIN } ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME)
    }

    /** Fetches the Hangar project with the given [slug]. */
    suspend fun getProject(slug: String): HangarProject? {
        val response =
            client.get("$baseUrl/projects/$slug") {
                token()?.let { header(HttpHeaders.Authorization, it) }
            }
        return when (response.status.value) {
            404 -> null
            !in 200..299 -> throw IOException("Failed to fetch project: ${response.status.value} ${response.bodyAsText()}")
            else -> response.body()
        }
    }

    /** Fetches all versions of the Hangar project with the given [slug]. */
    suspend fun getVersions(slug: String): List<HangarVersion> {
        val response =
            client.get("$baseUrl/projects/$slug/versions") {
                token()?.let { header(HttpHeaders.Authorization, it) }
            }
        return when (response.status.value) {
            404 -> emptyList()
            !in 200..299 -> throw IOException("Failed to fetch versions: ${response.status.value} ${response.bodyAsText()}")
            else -> response.body()
        }
    }

    /** Fetches all channels of the Hangar project with the given [slug]. */
    suspend fun getChannels(slug: String): List<HangarChannel> {
        val response =
            client.get("$baseUrl/projects/$slug/channels") {
                token()?.let { header(HttpHeaders.Authorization, it) }
            }
        return when (response.status.value) {
            404 -> emptyList()
            !in 200..299 -> throw IOException("Failed to fetch channels: ${response.status.value} ${response.bodyAsText()}")
            else -> response.body()
        }
    }

    /** Creates a new release [name] channel on the project if it does not exist. */
    suspend fun createChannel(
        slug: String,
        name: String,
    ) {
        val response =
            client.post("$baseUrl/projects/$slug/channels") {
                token()?.let { header(HttpHeaders.Authorization, it) }
                contentType(ContentType.Application.Json)
                setBody(HangarChannel(name))
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to create channel '$name': ${response.status.value} ${response.bodyAsText()}")
        }
    }

    /** Updates Hangar project metadata. */
    suspend fun updateProject(
        slug: String,
        name: String,
        summary: String,
        description: String,
        license: String,
        tags: List<String>,
        links: TerracottaProjectLinks? = null,
    ) {
        val body: JsonObject =
            buildJsonObject {
                put("name", name)
                put("description", summary)
                put("body", description)
                put("license", license)
                put("tags", buildJsonArray { tags.forEach { add(it) } })
                links?.homepage?.let { put("homepage", it) }
                links?.source?.let { put("source", it) }
                links?.issues?.let { put("issues", it) }
                links?.wiki?.let { put("wiki", it) }
                links?.community?.let { put("discord", it) }
                if (links?.donations?.isNotEmpty() == true) {
                    put(
                        "donations",
                        buildJsonArray {
                            links.donations.forEach { donation ->
                                addJsonObject {
                                    put("platform", donation.platform)
                                    put("url", donation.url)
                                }
                            }
                        },
                    )
                }
            }

        val response =
            client.patch("$baseUrl/projects/$slug") {
                token()?.let { header(HttpHeaders.Authorization, it) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        if (response.status.value !in 200..299) {
            throw IOException("Failed to update project: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully updated project metadata on Hangar.")
    }

    /** Deletes the Hangar project identified by [slug]. */
    suspend fun deleteProject(slug: String) {
        val response =
            client.delete("$baseUrl/projects/$slug") {
                token()?.let { header(HttpHeaders.Authorization, it) }
            }
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw IOException("Failed to delete project: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully deleted project $slug on Hangar.")
    }

    /** Deletes the Hangar version identified by [versionId] on the project [slug]. */
    suspend fun deleteVersion(
        slug: String,
        versionId: String,
    ) {
        val response =
            client.delete("$baseUrl/projects/$slug/versions/$versionId") {
                token()?.let { header(HttpHeaders.Authorization, it) }
            }
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw IOException("Failed to delete version: ${response.status.value} ${response.bodyAsText()}")
        }
        logger.info("Successfully deleted version $versionId of project $slug on Hangar.")
    }

    /** Uploads [version] to the Hangar project identified by [slug]. */
    suspend fun uploadVersion(
        slug: String,
        version: TerracottaVersion,
    ) {
        if (version.environment == TerracottaEnvironment.CLIENT_ONLY) {
            logger.warn("Hangar is implicitly server-only; ignoring CLIENT_ONLY environment for version ${version.version}.")
        }

        val platforms = loaderMapper.mapToPlatforms(version.loaders)
        if (platforms.isEmpty()) {
            throw IOException("No supported Hangar platforms for version ${version.version}")
        }

        val channel = version.releaseType.toHangarChannel()
        ensureChannelExists(slug, channel)

        val platformDependencies = platforms.associateWith { version.gameVersions }
        val versionUpload: JsonObject =
            buildJsonObject {
                put("version", version.version)
                put("channel", channel)
                put("description", version.changelog)
                put(
                    "platformDependencies",
                    buildJsonObject {
                        platformDependencies.forEach { (platform, versions) ->
                            put(platform, buildJsonArray { versions.forEach { add(it) } })
                        }
                    },
                )
                put("dependencies", buildJsonObject { })
            }

        val artifactFile = File(version.artifactPath)
        if (!artifactFile.exists()) {
            throw IOException("Artifact file not found: ${version.artifactPath}")
        }

        val response =
            client.post("$baseUrl/projects/$slug/upload") {
                token()?.let { header(HttpHeaders.Authorization, it) }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("versionUpload", json.encodeToString(JsonObject.serializer(), versionUpload))
                            append(
                                "file",
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
        logger.info("Successfully uploaded version ${version.version} to Hangar.")
    }

    private suspend fun ensureChannelExists(
        slug: String,
        channel: String,
    ) {
        val channels = getChannels(slug)
        if (channels.any { it.name.equals(channel, ignoreCase = true) }) return
        createChannel(slug, channel)
    }

    companion object {
        private const val REFRESH_MARGIN = 60_000L
        private const val DEFAULT_TOKEN_LIFETIME = 3_600_000L

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
}

private fun TerracottaReleaseType.toHangarChannel(): String {
    return when (this) {
        TerracottaReleaseType.RELEASE -> "Release"
        TerracottaReleaseType.BETA -> "Snapshot"
        TerracottaReleaseType.ALPHA -> "Snapshot"
    }
}
