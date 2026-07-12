package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthLicense
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthProject
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersion
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersionFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class ModrinthClientTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private fun createClient(engine: MockEngine): HttpClient {
        return HttpClient(engine) {
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

    @Test
    fun `getProject returns project on success`() =
        runTest {
            val project =
                ModrinthProject(
                    id = "abc",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "Summary",
                    body = "Body",
                    categories = listOf("utility"),
                    license = ModrinthLicense("MIT"),
                )
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(json.encodeToString(project)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val result = client.getProject("my-mod")
            assertNotNull(result)
            assertEquals("abc", result?.id)
            assertEquals("My Mod", result?.title)
        }

    @Test
    fun `getProject returns null on 404`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("", HttpStatusCode.NotFound) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            assertNull(client.getProject("missing"))
        }

    @Test
    fun `getProject throws IOException on error`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("Internal error", HttpStatusCode.InternalServerError) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val exception =
                try {
                    client.getProject("my-mod")
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Failed to fetch project"))
        }

    @Test
    fun `getVersions returns versions on success`() =
        runTest {
            val versions =
                listOf(
                    ModrinthVersion(
                        versionNumber = "1.0.0",
                        gameVersions = listOf("1.20"),
                        files = listOf(ModrinthVersionFile(url = "", filename = "mod.jar", primary = true)),
                    ),
                )
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(json.encodeToString(versions)),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val result = client.getVersions("my-mod")
            assertEquals(1, result.size)
            assertEquals("1.0.0", result.first().versionNumber)
        }

    @Test
    fun `getVersions returns empty list on 404`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("", HttpStatusCode.NotFound) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            assertTrue(client.getVersions("missing").isEmpty())
        }

    @Test
    fun `getVersions throws IOException on error`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("boom", HttpStatusCode.BadRequest) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val exception =
                try {
                    client.getVersions("my-mod")
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Failed to fetch versions"))
        }

    @Test
    fun `patchProject succeeds and sends auth header`() =
        runTest {
            var patched = false
            var authHeader: String? = null
            val mockEngine =
                MockEngine { request ->
                    if (request.url.encodedPath == "/project/my-mod" && request.method == HttpMethod.Patch) {
                        patched = true
                        authHeader = request.headers[HttpHeaders.Authorization]
                        respond("", HttpStatusCode.OK)
                    } else {
                        respond("", HttpStatusCode.NotFound)
                    }
                }
            val client = ModrinthClient(token = "my-token", baseUrl = "http://localhost", client = createClient(mockEngine))
            client.patchProject("my-mod", mapOf("title" to "New Title"))
            assertTrue(patched)
            assertEquals("my-token", authHeader)
        }

    @Test
    fun `patchProject throws IOException on error`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("Conflict", HttpStatusCode.Conflict) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val exception =
                try {
                    client.patchProject("my-mod", mapOf("title" to "X"))
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Failed to patch project"))
        }

    @Test
    fun `createVersion uploads multipart body`() =
        runTest {
            val tempDir = Files.createTempDirectory("modrinth").toFile()
            tempDir.deleteOnExit()
            val artifact = File(tempDir, "mod.jar")
            artifact.deleteOnExit()
            artifact.writeText("content")

            var uploaded = false
            var authHeader: String? = null
            val mockEngine =
                MockEngine { request ->
                    if (request.url.encodedPath == "/version" && request.method == HttpMethod.Post) {
                        uploaded = true
                        authHeader = request.headers[HttpHeaders.Authorization]
                        val body = String(request.body.toByteArray())
                        assertTrue(body.contains("""version_number":"1.0.0"""))
                        assertTrue(body.contains("""project_id":"my-mod"""))
                        assertTrue(body.contains("filename=\"mod.jar\""))
                        respond(
                            content =
                                ByteReadChannel(
                                    json.encodeToString(
                                        ModrinthVersion(
                                            versionNumber = "1.0.0",
                                            gameVersions = listOf("1.20"),
                                            files = emptyList(),
                                        ),
                                    ),
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond("", HttpStatusCode.OK)
                    }
                }
            val client = ModrinthClient(token = "my-token", baseUrl = "http://localhost", client = createClient(mockEngine))
            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = artifact.absolutePath,
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                    changelog = "Notes",
                )
            client.createVersion("my-mod", version)
            assertTrue(uploaded)
            assertEquals("my-token", authHeader)
        }

    @Test
    fun `createVersion throws IOException when artifact missing`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("", HttpStatusCode.OK) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = "/nonexistent/artifact.jar",
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                )
            val exception =
                try {
                    client.createVersion("my-mod", version)
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Artifact file not found"))
        }

    @Test
    fun `createProject returns created project id and maps environment fields`() =
        runTest {
            val cases =
                listOf(
                    Triple(TerracottaEnvironment.CLIENT_ONLY, "required", "unsupported"),
                    Triple(TerracottaEnvironment.SERVER_ONLY, "optional", "required"),
                    Triple(TerracottaEnvironment.UNIVERSAL, "required", "required"),
                )

            for ((environment, expectedClientSide, expectedServerSide) in cases) {
                val version =
                    TerracottaVersion(
                        version = "1.0.0",
                        artifactPath = "",
                        gameVersions = listOf("1.20"),
                        loaders = listOf("fabric"),
                        environment = environment,
                    )
                val project =
                    TerracottaProject(
                        id = "my-mod",
                        name = "My Mod",
                        summary = "Summary",
                        description = "Body",
                        versions = listOf(version),
                        tags = listOf("utility"),
                        license = "MIT",
                    )
                val createdProject =
                    ModrinthProject(
                        id = "created-id",
                        slug = "my-mod",
                        title = "My Mod",
                        summary = "Summary",
                        body = "Body",
                        categories = listOf("utility"),
                        license = ModrinthLicense("MIT"),
                    )
                var capturedBody: String? = null
                val mockEngine =
                    MockEngine { request ->
                        when {
                            request.url.encodedPath == "/project" && request.method == HttpMethod.Post -> {
                                capturedBody = String(request.body.toByteArray())
                                respond(
                                    content = ByteReadChannel(json.encodeToString(createdProject)),
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            else -> respond("", HttpStatusCode.OK)
                        }
                    }
                val client = ModrinthClient(token = "my-token", baseUrl = "http://localhost", client = createClient(mockEngine))
                val id = client.createProject(project)
                assertEquals("created-id", id)
                assertNotNull(capturedBody)
                assertTrue(capturedBody!!.contains("""client_side":"$expectedClientSide"""))
                assertTrue(capturedBody.contains("""server_side":"$expectedServerSide"""))
            }
        }

    @Test
    fun `createProject throws IOException on error`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("Invalid slug", HttpStatusCode.BadRequest) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val project =
                TerracottaProject(
                    id = "my-mod",
                    name = "My Mod",
                    summary = "Summary",
                    description = "Body",
                    versions = emptyList(),
                    tags = emptyList(),
                    license = "MIT",
                )
            val exception =
                try {
                    client.createProject(project)
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Failed to create project"))
        }

    @Test
    fun `createProject emits license_url when licenseUrl is set`() =
        runTest {
            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = "",
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                    environment = TerracottaEnvironment.SERVER_ONLY,
                )
            val project =
                TerracottaProject(
                    id = "my-mod",
                    name = "My Mod",
                    summary = "Summary",
                    description = "Body",
                    versions = listOf(version),
                    tags = listOf("utility"),
                    license = "MIT",
                    licenseUrl = "https://example.com/LICENSE",
                )
            val createdProject =
                ModrinthProject(
                    id = "created-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "Summary",
                    body = "Body",
                    categories = listOf("utility"),
                    license = ModrinthLicense("MIT"),
                )
            var capturedBody: String? = null
            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/project" && request.method == HttpMethod.Post -> {
                            capturedBody = String(request.body.toByteArray())
                            respond(
                                content = ByteReadChannel(json.encodeToString(createdProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> respond("", HttpStatusCode.OK)
                    }
                }
            val client = ModrinthClient(token = "my-token", baseUrl = "http://localhost", client = createClient(mockEngine))
            val id = client.createProject(project)
            assertEquals("created-id", id)
            assertNotNull(capturedBody)
            assertTrue(capturedBody!!.contains("""license_url":"https://example.com/LICENSE"""))
        }

    @Test
    fun `uploadIcon patches icon endpoint with multipart body`() =
        runTest {
            val tempDir = Files.createTempDirectory("modrinth-icon").toFile()
            tempDir.deleteOnExit()
            val icon = File(tempDir, "icon.png")
            icon.deleteOnExit()
            icon.writeText("icon-content")

            var uploaded = false
            var authHeader: String? = null
            var extParam: String? = null
            val mockEngine =
                MockEngine { request ->
                    if (request.url.encodedPath == "/project/my-mod/icon" && request.method == HttpMethod.Patch) {
                        uploaded = true
                        authHeader = request.headers[HttpHeaders.Authorization]
                        extParam = request.url.parameters["ext"]
                        val body = String(request.body.toByteArray())
                        assertTrue(body.contains("filename=\"icon.png\""))
                        assertTrue(body.contains("icon-content"))
                        respond("", HttpStatusCode.NoContent)
                    } else {
                        respond("", HttpStatusCode.NotFound)
                    }
                }
            val client = ModrinthClient(token = "my-token", baseUrl = "http://localhost", client = createClient(mockEngine))
            client.uploadIcon("my-mod", icon.absolutePath)
            assertTrue(uploaded)
            assertEquals("my-token", authHeader)
            assertEquals("png", extParam)
        }

    @Test
    fun `uploadIcon throws IOException when icon file is missing`() =
        runTest {
            val mockEngine = MockEngine { _ -> respond("", HttpStatusCode.OK) }
            val client = ModrinthClient(token = null, baseUrl = "http://localhost", client = createClient(mockEngine))
            val exception =
                try {
                    client.uploadIcon("my-mod", "/nonexistent/icon.png")
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message!!.contains("Gallery image not found"))
        }
}
