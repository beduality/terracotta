package io.github.beduality.terracotta.provider.modrinth.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.beduality.terracotta.core.model.TerracottaVersion
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthProject
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersion
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class ModrinthClient(
    private val token: String?,
    private val baseUrl: String = "https://api.modrinth.com/v2",
) {
    private val client = OkHttpClient()
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(ModrinthClient::class.java)

    private fun Request.Builder.auth(): Request.Builder {
        if (!token.isNullOrBlank()) {
            return this.header("Authorization", token)
        }
        return this
    }

    fun getProject(projectIdOrSlug: String): ModrinthProject? {
        val request =
            Request.Builder()
                .url("$baseUrl/project/$projectIdOrSlug")
                .auth()
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch project: ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body")
            return mapper.readValue(body)
        }
    }

    fun getVersions(projectIdOrSlug: String): List<ModrinthVersion> {
        val request =
            Request.Builder()
                .url("$baseUrl/project/$projectIdOrSlug/version")
                .auth()
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return emptyList()
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch versions: ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body")
            return mapper.readValue(body)
        }
    }

    fun patchProject(
        projectIdOrSlug: String,
        patchData: Map<String, Any>,
    ) {
        val json = mapper.writeValueAsString(patchData)
        val request =
            Request.Builder()
                .url("$baseUrl/project/$projectIdOrSlug")
                .auth()
                .patch(json.toRequestBody("application/json".toMediaType()))
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to patch project: ${response.code} ${response.body?.string()}")
            }
            logger.info("Successfully updated project metadata on Modrinth.")
        }
    }

    fun createVersion(
        projectId: String,
        version: TerracottaVersion,
    ) {
        val artifactFile = File(version.artifactPath)
        if (!artifactFile.exists()) {
            throw IOException("Artifact file not found: ${version.artifactPath}")
        }

        // Prepare "data" body part
        val dataPart =
            mapOf(
                "name" to "Version ${version.version}",
                "version_number" to version.version,
                "game_versions" to version.gameVersions,
                "loaders" to version.loaders,
                "project_id" to projectId,
                "file_parts" to listOf("file_0"),
                "changelog" to "Uploaded via Terracotta declarative deployment.",
            )
        val dataJson = mapper.writeValueAsString(dataPart)

        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", dataJson)
                .addFormDataPart(
                    "file_0",
                    artifactFile.name,
                    artifactFile.asRequestBody("application/java-archive".toMediaType()),
                )
                .build()

        val request =
            Request.Builder()
                .url("$baseUrl/version")
                .auth()
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to upload version: ${response.code} ${response.body?.string()}")
            }
            logger.info("Successfully uploaded version ${version.version} to Modrinth.")
        }
    }

    fun createProject(project: io.github.beduality.terracotta.core.model.TerracottaProject) {
        val dataPart =
            mapOf(
                "slug" to project.id,
                "title" to project.name,
                "description" to project.summary,
                "body" to project.description,
                "categories" to project.tags,
                "client_side" to "optional",
                "server_side" to "required",
                "project_type" to "mod",
                "license_id" to project.license.lowercase(),
            )
        val dataJson = mapper.writeValueAsString(dataPart)

        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", dataJson)
                .build()

        val request =
            Request.Builder()
                .url("$baseUrl/project")
                .auth()
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to create project: ${response.code} ${response.body?.string()}")
            }
            logger.info("Successfully created project ${project.name} on Modrinth.")
        }
    }
}
