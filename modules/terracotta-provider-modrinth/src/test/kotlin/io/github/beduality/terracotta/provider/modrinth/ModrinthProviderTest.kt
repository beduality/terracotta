package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.logic.ModrinthProviderLogic
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthGalleryItem
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthLicense
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthProject
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersion
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersionFile
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream

class ModrinthProviderTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `test ModrinthStateProvider fetch existing project`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT"),
                )
            val modrinthVersion =
                ModrinthVersion(
                    versionNumber = "1.0.0",
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                    files =
                        listOf(
                            ModrinthVersionFile(
                                url = "",
                                filename = "mod.jar",
                                primary = true,
                            ),
                        ),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            val responseJson = json.encodeToString(modrinthProject)
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            val responseJson = json.encodeToString(listOf(modrinthVersion))
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("my-mod-id")

            assertNotNull(terracottaProject)
            assertEquals("my-mod-id", terracottaProject?.id)
            assertEquals("My Mod", terracottaProject?.name)
            assertEquals("A test mod", terracottaProject?.summary)
            assertEquals("Test description", terracottaProject?.description)
            assertEquals(listOf("utility"), terracottaProject?.tags)
            assertEquals("MIT", terracottaProject?.license)
            assertEquals(1, terracottaProject?.versions?.size)
            assertEquals("1.0.0", terracottaProject?.versions?.firstOrNull()?.version)
            assertEquals(listOf("fabric"), terracottaProject?.versions?.firstOrNull()?.loaders)
            assertEquals(TerracottaReleaseType.RELEASE, terracottaProject?.versions?.firstOrNull()?.releaseType)
            assertEquals("", terracottaProject?.versions?.firstOrNull()?.changelog)
        }

    @Test
    fun `test ModrinthStateProvider fetch non-existing project returns null`() =
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("non-existent-id")
            assertNull(terracottaProject)
        }

    @Test
    fun `test ModrinthRegistryProvider apply UploadVersion`() =
        runTest {
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            var createVersionCalled = false
            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = tempFile.absolutePath,
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/version" && request.method == HttpMethod.Post -> {
                            createVersionCalled = true
                            val responseVersion =
                                ModrinthVersion(
                                    versionNumber = "1.0.0",
                                    gameVersions = listOf("1.20"),
                                    loaders = listOf("fabric"),
                                    files = emptyList(),
                                )
                            respond(
                                content = ByteReadChannel(json.encodeToString(responseVersion)),
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            registryProvider.apply("my-mod", listOf(Operation.UploadVersion(version)))

            assertTrue(createVersionCalled)
        }

    @Test
    fun `test ModrinthRegistryProvider apply metadata updates`() =
        runTest {
            val capturedPatches = mutableListOf<Pair<String, String>>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/project/my-mod" && request.method == HttpMethod.Patch -> {
                            val body = (request.body as TextContent).text
                            capturedPatches.add(request.url.encodedPath to body)
                            respond("", status = HttpStatusCode.OK)
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            val operations =
                listOf(
                    Operation.UpdateMetadata(
                        nameChanged = true,
                        summaryChanged = true,
                        licenseChanged = true,
                        licenseUrlChanged = false,
                        newName = "New Name",
                        newSummary = "New summary",
                        newLicense = "Apache-2.0",
                        newLicenseUrl = null,
                    ),
                    Operation.UpdateDescription(oldDescription = "Old body", newDescription = "New body"),
                    Operation.UpdateTags(oldTags = listOf("old"), newTags = listOf("new", "tag")),
                )

            registryProvider.apply("my-mod", operations)

            assertEquals(3, capturedPatches.size)
            val metadataBody = json.parseToJsonElement(capturedPatches[0].second).jsonObject
            assertEquals("New Name", metadataBody["title"]?.jsonPrimitive?.content)
            assertEquals("New summary", metadataBody["description"]?.jsonPrimitive?.content)
            assertEquals("Apache-2.0", metadataBody["license_id"]?.jsonPrimitive?.content)

            val descriptionBody = json.parseToJsonElement(capturedPatches[1].second).jsonObject
            assertEquals("New body", descriptionBody["body"]?.jsonPrimitive?.content)

            val tagsBody = json.parseToJsonElement(capturedPatches[2].second).jsonObject
            assertEquals(listOf("new", "tag"), tagsBody["categories"]?.jsonArray?.map { it.jsonPrimitive.content })
        }

    @Test
    fun `test ModrinthRegistryProvider apply CreateProject updates resolved project id`() =
        runTest {
            val createdProject =
                ModrinthProject(
                    id = "new-project-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT"),
                )
            val capturedPaths = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/project" && request.method == HttpMethod.Post -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(createdProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath == "/project/new-project-id" && request.method == HttpMethod.Patch -> {
                            capturedPaths.add(request.url.encodedPath)
                            respond("", status = HttpStatusCode.OK)
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            val project =
                TerracottaProject(
                    id = "my-mod",
                    name = "My Mod",
                    summary = "A test mod",
                    description = "Test description",
                    versions = emptyList(),
                    tags = listOf("utility"),
                    license = "MIT",
                )

            registryProvider.apply(
                "my-mod",
                listOf(
                    Operation.CreateProject(project),
                    Operation.UpdateDescription(oldDescription = "Old body", newDescription = "New body"),
                ),
            )

            assertEquals(listOf("/project/new-project-id"), capturedPaths)
        }

    @Test
    fun `test ModrinthRegistryProvider does nothing when operations list is empty`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine { _ ->
                    requestCount++
                    respond("", status = HttpStatusCode.OK)
                }

            val client = HttpClient(mockEngine)
            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            registryProvider.apply("my-mod", emptyList())

            assertEquals(0, requestCount)
        }

    @Test
    fun `test ModrinthStateProvider maps beta release type and empty file list`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT"),
                )
            val modrinthVersion =
                ModrinthVersion(
                    versionNumber = "2.0.0-beta",
                    gameVersions = listOf("1.21"),
                    loaders = listOf("fabric", "quilt"),
                    files = emptyList(),
                    versionType = "beta",
                    changelog = "Beta notes",
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(modrinthProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(listOf(modrinthVersion))),
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("my-mod-id")

            assertNotNull(terracottaProject)
            val version = terracottaProject?.versions?.firstOrNull()
            assertEquals("2.0.0-beta", version?.version)
            assertEquals("", version?.artifactPath)
            assertEquals(listOf("1.21"), version?.gameVersions)
            assertEquals(listOf("fabric", "quilt"), version?.loaders)
            assertEquals(TerracottaReleaseType.BETA, version?.releaseType)
            assertEquals("Beta notes", version?.changelog)
        }

    @Test
    fun `test ModrinthDestructiveRegistryProvider deleteProject sends DELETE request`() =
        runTest {
            val deletedPaths = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    if (request.method == HttpMethod.Delete && request.url.encodedPath == "/project/my-mod") {
                        deletedPaths.add(request.url.encodedPath)
                        respond("", status = HttpStatusCode.NoContent)
                    } else {
                        respond("", status = HttpStatusCode.NotFound)
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val destructiveProvider = ModrinthDestructiveRegistryProvider(modrinthClient)

            destructiveProvider.deleteProject("my-mod")

            assertEquals(listOf("/project/my-mod"), deletedPaths)
        }

    @Test
    fun `test ModrinthDestructiveRegistryProvider deleteAllVersions deletes each version`() =
        runTest {
            val deletedVersionIds = mutableListOf<String>()

            val versions =
                listOf(
                    ModrinthVersion(
                        id = "version-a",
                        versionNumber = "1.0.0",
                        gameVersions = listOf("1.20"),
                        loaders = listOf("fabric"),
                        files = emptyList(),
                    ),
                    ModrinthVersion(
                        id = "version-b",
                        versionNumber = "1.1.0",
                        gameVersions = listOf("1.20"),
                        loaders = listOf("fabric"),
                        files = emptyList(),
                    ),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/project/my-mod/version" && request.method == HttpMethod.Get -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(versions)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/version/version-a" -> {
                            deletedVersionIds.add("version-a")
                            respond("", status = HttpStatusCode.NoContent)
                        }
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/version/version-b" -> {
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val destructiveProvider = ModrinthDestructiveRegistryProvider(modrinthClient)

            destructiveProvider.deleteAllVersions("my-mod")

            assertEquals(listOf("version-a", "version-b"), deletedVersionIds)
        }

    @Test
    fun `test ModrinthProviderFactory creates destructive registry provider`() {
        val factory = ModrinthProviderFactory()
        val provider = factory.createDestructiveRegistryProvider("test-token")

        assertNotNull(provider)
        assertTrue(provider is ModrinthDestructiveRegistryProvider)
    }

    @Test
    fun `test ModrinthStateProvider maps gallery items`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT"),
                    gallery =
                        listOf(
                            ModrinthGalleryItem(
                                url = "https://cdn.modrinth.com/data/image1.png",
                                title = "Screenshot one",
                                description = "First screenshot",
                                featured = true,
                                ordering = 0,
                            ),
                        ),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            val responseJson = json.encodeToString(modrinthProject)
                            respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            respond(
                                content = ByteReadChannel("[]"),
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("my-mod-id")

            assertEquals(1, terracottaProject?.gallery?.size)
            val item = terracottaProject?.gallery?.first()
            assertEquals("https://cdn.modrinth.com/data/image1.png", item?.imagePath)
            assertEquals("Screenshot one", item?.title)
            assertEquals("First screenshot", item?.description)
            assertEquals(true, item?.featured)
            assertEquals(0, item?.ordering)
        }

    @Test
    fun `test ModrinthRegistryProvider uploads gallery item`() =
        runTest {
            val uploadedPaths = mutableListOf<String>()
            val imageFile = File.createTempFile("gallery", ".png")
            imageFile.deleteOnExit()
            imageFile.writeBytes(ByteArray(10))

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.method == HttpMethod.Post && request.url.encodedPath == "/project/my-mod/gallery" -> {
                            uploadedPaths.add(request.url.encodedPath)
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            val item =
                io.github.beduality.terracotta.core.model.TerracottaGalleryItem(
                    imagePath = imageFile.absolutePath,
                    title = "My Image",
                )
            registryProvider.apply("my-mod", listOf(Operation.UploadGalleryItem(item)))

            assertEquals(listOf("/project/my-mod/gallery"), uploadedPaths)
        }

    @Test
    fun `test ModrinthRegistryProvider deletes gallery item by url`() =
        runTest {
            val deletedUrls = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.method == HttpMethod.Delete && request.url.encodedPath == "/project/my-mod/gallery" -> {
                            request.url.parameters["url"]?.let { deletedUrls.add(it) }
                            respond("", status = HttpStatusCode.NoContent)
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            val item =
                io.github.beduality.terracotta.core.model.TerracottaGalleryItem(
                    imagePath = "https://cdn/image.png",
                    title = "My Image",
                )
            registryProvider.apply("my-mod", listOf(Operation.DeleteGalleryItem(item)))

            assertEquals(listOf("https://cdn/image.png"), deletedUrls)
        }

    @Test
    fun `test ModrinthStateProvider maps licenseUrl`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT", url = "https://example.com/LICENSE"),
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(modrinthProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            respond(
                                content = ByteReadChannel("[]"),
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("my-mod-id")

            assertEquals("https://example.com/LICENSE", terracottaProject?.licenseUrl)
        }

    @Test
    fun `test ModrinthRegistryProvider apply licenseUrl update sends license_url patch`() =
        runTest {
            val capturedPatches = mutableListOf<Pair<String, String>>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath == "/project/my-mod" && request.method == HttpMethod.Patch -> {
                            val body = (request.body as TextContent).text
                            capturedPatches.add(request.url.encodedPath to body)
                            respond("", status = HttpStatusCode.OK)
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            val operations =
                listOf(
                    Operation.UpdateMetadata(
                        nameChanged = false,
                        summaryChanged = false,
                        licenseChanged = false,
                        licenseUrlChanged = true,
                        newName = "Name",
                        newSummary = "Summary",
                        newLicense = "MIT",
                        newLicenseUrl = "https://example.com/LICENSE",
                    ),
                )

            registryProvider.apply("my-mod", operations)

            assertEquals(1, capturedPatches.size)
            val metadataBody = json.parseToJsonElement(capturedPatches[0].second).jsonObject
            assertEquals("https://example.com/LICENSE", metadataBody["license_url"]?.jsonPrimitive?.content)
        }

    @Test
    fun `test ModrinthStateProvider maps iconUrl`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    slug = "my-mod",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT"),
                    iconUrl = "https://cdn.modrinth.com/data/icon.png",
                )

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            respond(
                                content = ByteReadChannel(json.encodeToString(modrinthProject)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            respond(
                                content = ByteReadChannel("[]"),
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

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("my-mod-id")

            assertEquals("https://cdn.modrinth.com/data/icon.png", terracottaProject?.icon)
        }

    @Test
    fun `test ModrinthRegistryProvider uploads icon on UploadIcon`() =
        runTest {
            val iconFile = File.createTempFile("icon", ".png")
            iconFile.deleteOnExit()
            iconFile.writeBytes(ByteArray(10))

            val uploadedPaths = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.method == HttpMethod.Patch && request.url.encodedPath == "/project/my-mod/icon" -> {
                            uploadedPaths.add(request.url.encodedPath)
                            respond("", status = HttpStatusCode.NoContent)
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            registryProvider.apply("my-mod", listOf(Operation.UploadIcon(iconFile.absolutePath)))

            assertEquals(listOf("/project/my-mod/icon"), uploadedPaths)
        }

    @Test
    fun `test ModrinthRegistryProvider uploads icon on UpdateIcon`() =
        runTest {
            val iconFile = File.createTempFile("icon", ".png")
            iconFile.deleteOnExit()
            iconFile.writeBytes(ByteArray(10))

            val uploadedPaths = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.method == HttpMethod.Patch && request.url.encodedPath == "/project/my-mod/icon" -> {
                            uploadedPaths.add(request.url.encodedPath)
                            respond("", status = HttpStatusCode.NoContent)
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

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient, ModrinthProviderLogic)

            registryProvider.apply(
                "my-mod",
                listOf(
                    Operation.UpdateIcon(
                        oldIconUrl = "https://cdn.modrinth.com/data/old.png",
                        iconPath = iconFile.absolutePath,
                    ),
                ),
            )

            assertEquals(listOf("/project/my-mod/icon"), uploadedPaths)
        }
}
