package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import io.github.beduality.terracotta.provider.hangar.model.HangarChannel
import io.github.beduality.terracotta.provider.hangar.model.HangarProject
import io.github.beduality.terracotta.provider.hangar.model.HangarVersion
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HangarProviderTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `test HangarStateProvider fetch existing project`() =
        runTest {
            val hangarProject =
                HangarProject(
                    name = "My Plugin",
                    description = "A test plugin",
                    body = "Test description",
                    tags = listOf("utility"),
                    license = "MIT",
                )
            val hangarVersion =
                HangarVersion(
                    version = "1.0.0",
                    channel = "Release",
                    description = "Release notes",
                    platformDependencies = mapOf("PAPER" to listOf("1.20.1")),
                    fileName = "plugin.jar",
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/projects/my-plugin") -> {
                            val responseJson = json.encodeToString(hangarProject)
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/versions") -> {
                            val responseJson = json.encodeToString(listOf(hangarVersion))
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            respond("", status = HttpStatusCode.NotFound)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val stateProvider = HangarStateProvider(hangarClient)

            val terracottaProject = stateProvider.fetchProject("my-plugin")

            assertNotNull(terracottaProject)
            assertEquals("my-plugin", terracottaProject?.id)
            assertEquals("My Plugin", terracottaProject?.name)
            assertEquals("A test plugin", terracottaProject?.summary)
            assertEquals("Test description", terracottaProject?.description)
            assertEquals(listOf("utility"), terracottaProject?.tags)
            assertEquals("MIT", terracottaProject?.license)
            assertEquals(1, terracottaProject?.versions?.size)
            assertEquals("1.0.0", terracottaProject?.versions?.firstOrNull()?.version)
            assertEquals(listOf("paper"), terracottaProject?.versions?.firstOrNull()?.loaders)
            assertEquals(listOf("1.20.1"), terracottaProject?.versions?.firstOrNull()?.gameVersions)
            assertEquals(TerracottaReleaseType.RELEASE, terracottaProject?.versions?.firstOrNull()?.releaseType)
        }

    @Test
    fun `test HangarStateProvider fetch non-existing project returns null`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond("", status = HttpStatusCode.NotFound)
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val stateProvider = HangarStateProvider(hangarClient)

            val terracottaProject = stateProvider.fetchProject("non-existent")
            assertNull(terracottaProject)
        }

    @Test
    fun `test HangarRegistryProvider apply UploadVersion`() =
        runTest {
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            var uploadCalled = false
            var channelChecked = false

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            val responseJson = json.encodeToString(mapOf("token" to "jwt-token"))
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/channels") -> {
                            channelChecked = true
                            val responseJson = json.encodeToString(listOf(HangarChannel("Release")))
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/upload") -> {
                            uploadCalled = true
                            respond(
                                content = ByteReadChannel(""),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            respond("", HttpStatusCode.OK)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = "test-api-key", baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = tempFile.absolutePath,
                    gameVersions = listOf("1.20.1"),
                    loaders = listOf("paper"),
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = "Release notes",
                )

            registryProvider.apply("my-plugin", listOf(io.github.beduality.terracotta.core.diff.Operation.UploadVersion(version)))

            assertEquals(true, channelChecked)
            assertEquals(true, uploadCalled)
        }

    @Test
    fun `test HangarRegistryProvider apply metadata updates`() =
        runTest {
            val currentProject =
                HangarProject(
                    name = "Old Name",
                    description = "Old summary",
                    body = "Old body",
                    tags = listOf("old"),
                    license = "MIT",
                )

            var patchBody: String? = null

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/projects/my-plugin") && request.method == HttpMethod.Get -> {
                            val responseJson = json.encodeToString(currentProject)
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin") && request.method == HttpMethod.Patch -> {
                            patchBody = (request.body as TextContent).text
                            respond("", status = HttpStatusCode.OK)
                        }
                        else -> respond("", status = HttpStatusCode.NotFound)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            val operations =
                listOf(
                    Operation.UpdateMetadata(
                        nameChanged = true,
                        summaryChanged = true,
                        licenseChanged = false,
                        licenseUrlChanged = false,
                        newName = "New Name",
                        newSummary = "New summary",
                        newLicense = "",
                        newLicenseUrl = null,
                    ),
                    Operation.UpdateDescription(oldDescription = "Old body", newDescription = "New body"),
                    Operation.UpdateTags(oldTags = listOf("old"), newTags = listOf("new", "tag")),
                )

            registryProvider.apply("my-plugin", operations)

            assertNotNull(patchBody)
            val body = json.parseToJsonElement(patchBody!!).jsonObject
            assertEquals("New Name", body["name"]?.jsonPrimitive?.content)
            assertEquals("New summary", body["description"]?.jsonPrimitive?.content)
            assertEquals("New body", body["body"]?.jsonPrimitive?.content)
            assertEquals("MIT", body["license"]?.jsonPrimitive?.content)
            assertEquals(listOf("new", "tag"), body["tags"]?.jsonArray?.map { it.jsonPrimitive.content })
        }

    @Test
    fun `test HangarRegistryProvider warns on CreateProject and skips`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine { _ ->
                    requestCount++
                    respond("", status = HttpStatusCode.OK)
                }

            val client = HttpClient(mockEngine)
            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            val project =
                TerracottaProject(
                    id = "my-plugin",
                    name = "My Plugin",
                    summary = "Summary",
                    description = "Description",
                    versions = emptyList(),
                    tags = emptyList(),
                    license = "MIT",
                    licenseUrl = "https://example.com/LICENSE",
                )

            registryProvider.apply("my-plugin", listOf(Operation.CreateProject(project)))

            assertEquals(0, requestCount)
        }

    @Test
    fun `test HangarRegistryProvider does nothing when operations list is empty`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine { _ ->
                    requestCount++
                    respond("", status = HttpStatusCode.OK)
                }

            val client = HttpClient(mockEngine)
            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            registryProvider.apply("my-plugin", emptyList())

            assertEquals(0, requestCount)
        }

    @Test
    fun `test HangarStateProvider maps snapshot and alpha channels`() =
        runTest {
            val project =
                HangarProject(
                    name = "My Plugin",
                    description = "Summary",
                    body = "Body",
                    tags = emptyList(),
                    license = "MIT",
                )
            val snapshotVersion =
                HangarVersion(
                    version = "1.1.0",
                    channel = "Snapshot",
                    description = "Snapshot notes",
                    platformDependencies = mapOf("PAPER" to listOf("1.20.2")),
                    fileName = "plugin.jar",
                )
            val alphaVersion =
                HangarVersion(
                    version = "1.2.0",
                    channel = "Alpha",
                    description = "Alpha notes",
                    platformDependencies = mapOf("VELOCITY" to listOf("3.3.0")),
                    fileName = "plugin.jar",
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/projects/my-plugin") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(project)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/versions") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(listOf(snapshotVersion, alphaVersion))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> respond("", status = HttpStatusCode.NotFound)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val stateProvider = HangarStateProvider(hangarClient)

            val terracottaProject = stateProvider.fetchProject("my-plugin")

            assertNotNull(terracottaProject)
            val versions = terracottaProject?.versions
            assertEquals(2, versions?.size)
            assertEquals(TerracottaReleaseType.BETA, versions?.first { it.version == "1.1.0" }?.releaseType)
            assertEquals(TerracottaReleaseType.ALPHA, versions?.first { it.version == "1.2.0" }?.releaseType)
        }

    @Test
    fun `test HangarClient caches JWT token`() =
        runTest {
            var authCalls = 0
            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            authCalls++
                            val authResponse =
                                buildJsonObject {
                                    put("token", "jwt-token")
                                    put("expiresAt", Long.MAX_VALUE)
                                }
                            respond(
                                content = ByteReadChannel(authResponse.toString()),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin") -> {
                            val project =
                                HangarProject(
                                    name = "My Plugin",
                                    description = "Summary",
                                    body = "Body",
                                    tags = emptyList(),
                                    license = "MIT",
                                )
                            respond(
                                content = ByteReadChannel(json.encodeToString(project)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            respond("", status = HttpStatusCode.OK)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            hangarClient.getProject("my-plugin")
            hangarClient.getProject("my-plugin")

            assertEquals(1, authCalls)
        }

    @Test
    fun `test HangarClient refreshes expired JWT token`() =
        runTest {
            var authCalls = 0
            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            authCalls++
                            val authResponse =
                                buildJsonObject {
                                    put("token", "jwt-token")
                                    put("expiresAt", 0L)
                                }
                            respond(
                                content = ByteReadChannel(authResponse.toString()),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin") -> {
                            val project =
                                HangarProject(
                                    name = "My Plugin",
                                    description = "Summary",
                                    body = "Body",
                                    tags = emptyList(),
                                    license = "MIT",
                                )
                            respond(
                                content = ByteReadChannel(json.encodeToString(project)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            respond("", status = HttpStatusCode.OK)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            hangarClient.getProject("my-plugin")
            hangarClient.getProject("my-plugin")

            assertEquals(2, authCalls)
        }

    @Test
    fun `test HangarClient uploadVersion creates channel when missing`() =
        runTest {
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            var channelCreated = false
            var uploadCalled = false

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(mapOf("token" to "jwt-token"))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/channels") && request.method == HttpMethod.Get -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(emptyList<HangarChannel>())),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/channels") && request.method == HttpMethod.Post -> {
                            channelCreated = true
                            respond("", status = HttpStatusCode.OK)
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/upload") -> {
                            uploadCalled = true
                            respond("", status = HttpStatusCode.OK)
                        }
                        else -> respond("", status = HttpStatusCode.OK)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = tempFile.absolutePath,
                    gameVersions = listOf("1.20.1"),
                    loaders = listOf("paper"),
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = "Release notes",
                )

            hangarClient.uploadVersion("my-plugin", version)

            assertEquals(true, channelCreated)
            assertEquals(true, uploadCalled)
        }

    @Test
    fun `test HangarClient uploadVersion throws when no supported platforms`() =
        runTest {
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            val mockEngine = MockEngine { _ -> respond("", status = HttpStatusCode.OK) }
            val client = HttpClient(mockEngine)
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = tempFile.absolutePath,
                    gameVersions = listOf("1.20.1"),
                    loaders = listOf("fabric", "forge"),
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = "Release notes",
                )

            val exception =
                try {
                    hangarClient.uploadVersion("my-plugin", version)
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message?.contains("No supported Hangar platforms") == true)
        }

    @Test
    fun `test HangarClient uploadVersion throws when artifact missing`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(mapOf("token" to "jwt-token"))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/channels") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(listOf(HangarChannel("Release")))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> respond("", status = HttpStatusCode.OK)
                    }
                }
            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = "/nonexistent/artifact.jar",
                    gameVersions = listOf("1.20.1"),
                    loaders = listOf("paper"),
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = "Release notes",
                )

            val exception =
                try {
                    hangarClient.uploadVersion("my-plugin", version)
                    null
                } catch (e: IOException) {
                    e
                }
            assertNotNull(exception)
            assertTrue(exception!!.message?.contains("Artifact file not found") == true)
        }

    @Test
    fun `test HangarClient uploadVersion ignores CLIENT_ONLY environment`() =
        runTest {
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            var uploadCalled = false

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(mapOf("token" to "jwt-token"))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/channels") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(listOf(HangarChannel("Release")))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin/upload") -> {
                            uploadCalled = true
                            respond("", status = HttpStatusCode.OK)
                        }
                        else -> respond("", status = HttpStatusCode.OK)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }
            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)

            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = tempFile.absolutePath,
                    gameVersions = listOf("1.20.1"),
                    loaders = listOf("paper"),
                    releaseType = TerracottaReleaseType.RELEASE,
                    changelog = "Release notes",
                    environment = TerracottaEnvironment.CLIENT_ONLY,
                )

            hangarClient.uploadVersion("my-plugin", version)

            assertEquals(true, uploadCalled)
        }

    @Test
    fun `test HangarDestructiveRegistryProvider deleteProject sends DELETE request`() =
        runTest {
            val deletedPaths = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(mapOf("token" to "jwt-token"))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/projects/my-plugin" -> {
                            deletedPaths.add(request.url.encodedPath)
                            respond("", status = HttpStatusCode.NoContent)
                        }
                        else -> {
                            respond("", status = HttpStatusCode.NotFound)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)
            val destructiveProvider = HangarDestructiveRegistryProvider(hangarClient)

            destructiveProvider.deleteProject("my-plugin")

            assertEquals(listOf("/projects/my-plugin"), deletedPaths)
        }

    @Test
    fun `test HangarDestructiveRegistryProvider deleteAllVersions deletes each version`() =
        runTest {
            val deletedVersionIds = mutableListOf<String>()

            val versions =
                listOf(
                    HangarVersion(
                        id = "version-a",
                        version = "1.0.0",
                        channel = "Release",
                    ),
                    HangarVersion(
                        id = "version-b",
                        version = "1.1.0",
                        channel = "Release",
                    ),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/authenticate") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(mapOf("token" to "jwt-token"))),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath == "/projects/my-plugin/versions" && request.method == HttpMethod.Get -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(versions)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/projects/my-plugin/versions/version-a" -> {
                            deletedVersionIds.add("version-a")
                            respond("", status = HttpStatusCode.NoContent)
                        }
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/projects/my-plugin/versions/version-b" -> {
                            deletedVersionIds.add("version-b")
                            respond("", status = HttpStatusCode.NoContent)
                        }
                        else -> {
                            respond("", status = HttpStatusCode.NotFound)
                        }
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)
            val destructiveProvider = HangarDestructiveRegistryProvider(hangarClient)

            destructiveProvider.deleteAllVersions("my-plugin")

            assertEquals(listOf("version-a", "version-b"), deletedVersionIds)
        }

    @Test
    fun `test HangarProviderFactory creates destructive registry provider`() {
        val factory = HangarProviderFactory()
        val provider = factory.createDestructiveRegistryProvider("test-key")

        assertNotNull(provider)
        assertTrue(provider is HangarDestructiveRegistryProvider)
    }

    @Test
    fun `test HangarRegistryProvider skips gallery operations without failing`() =
        runTest {
            var networkCalled = false

            val mockEngine =
                MockEngine { _ ->
                    networkCalled = true
                    respond("", status = HttpStatusCode.OK)
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = "test-key", baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            val item = io.github.beduality.terracotta.core.model.TerracottaGalleryItem(imagePath = "image.png", title = "My Image")
            registryProvider.apply(
                "my-plugin",
                listOf(
                    Operation.UploadGalleryItem(item),
                    Operation.UpdateGalleryItem(item, item),
                    Operation.DeleteGalleryItem(item),
                ),
            )

            assertEquals(false, networkCalled)
        }

    @Test
    fun `test HangarRegistryProvider ignores licenseUrl in UpdateMetadata`() =
        runTest {
            val currentProject =
                HangarProject(
                    name = "Old Name",
                    description = "Old summary",
                    body = "Old body",
                    tags = listOf("old"),
                    license = "MIT",
                )

            var patchBody: String? = null

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith("/projects/my-plugin") && request.method == HttpMethod.Get -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(currentProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.endsWith("/projects/my-plugin") && request.method == HttpMethod.Patch -> {
                            patchBody = (request.body as TextContent).text
                            respond("", status = HttpStatusCode.OK)
                        }
                        else -> respond("", status = HttpStatusCode.NotFound)
                    }
                }

            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                encodeDefaults = false
                            },
                        )
                    }
                }

            val hangarClient = HangarClient(apiKey = null, baseUrl = "http://localhost", client = client)
            val registryProvider = HangarRegistryProvider(hangarClient)

            registryProvider.apply(
                "my-plugin",
                listOf(
                    Operation.UpdateMetadata(
                        nameChanged = true,
                        summaryChanged = false,
                        licenseChanged = false,
                        licenseUrlChanged = true,
                        newName = "New Name",
                        newSummary = "",
                        newLicense = "",
                        newLicenseUrl = "https://example.com/LICENSE",
                    ),
                ),
            )

            assertNotNull(patchBody)
            val body = json.parseToJsonElement(patchBody!!).jsonObject
            assertEquals("New Name", body["name"]?.jsonPrimitive?.content)
            assertNull(body["licenseUrl"])
        }
}
