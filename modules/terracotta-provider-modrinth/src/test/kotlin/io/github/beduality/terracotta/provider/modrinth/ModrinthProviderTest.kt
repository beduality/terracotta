package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaVersion
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthLicense
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthProject
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersion
import io.github.beduality.terracotta.provider.modrinth.model.ModrinthVersionFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
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
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream

class ModrinthProviderTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    private fun createMockClient(
        mockHandler: suspend (MockEngine.MockRequestHandle) -> Unit,
    ): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = false
                    },
                )
            }
            engine {
                addHandler { request ->
                    mockHandler(this)
                    // Default response, handler should have set a response
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
    }

    @Test
    fun `test ModrinthStateProvider fetch existing project`() =
        runTest {
            val modrinthProject =
                ModrinthProject(
                    id = "my-mod-id",
                    title = "My Mod",
                    summary = "A test mod",
                    body = "Test description",
                    categories = listOf("utility"),
                    license = ModrinthLicense(id = "MIT", name = "MIT License"),
                )
            val modrinthVersion =
                ModrinthVersion(
                    id = "version-id",
                    versionNumber = "1.0.0",
                    gameVersions = listOf("1.20"),
                    loaders = listOf("fabric"),
                    files =
                        listOf(
                            ModrinthVersionFile(
                                hashes = mapOf(),
                                url = "",
                                filename = "mod.jar",
                                primary = true,
                                size = 1024,
                            ),
                        ),
                )

            val client =
                createMockClient { request ->
                    when {
                        request.url.encodedPath.contains("project/my-mod-id") &&
                            !request.url.encodedPath.contains("version") -> {
                            val responseJson = json.encodeToString(modrinthProject)
                            request.respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        request.url.encodedPath.contains("project/my-mod-id/version") -> {
                            val responseJson = json.encodeToString(listOf(modrinthVersion))
                            request.respond(
                                content = ByteReadChannel(responseJson),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            request.respond("", status = HttpStatusCode.NotFound)
                        }
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
        }

    @Test
    fun `test ModrinthStateProvider fetch non-existing project returns null`() =
        runTest {
            val client =
                createMockClient { request ->
                    request.respond("", status = HttpStatusCode.NotFound)
                }

            val modrinthClient = ModrinthClient(token = null, baseUrl = "http://localhost", client = client)
            val stateProvider = ModrinthStateProvider(modrinthClient)

            val terracottaProject = stateProvider.fetchProject("non-existent-id")
            assertNull(terracottaProject)
        }

    @Test
    fun `test ModrinthRegistryProvider apply UploadVersion`() =
        runTest {
            // Create a temporary jar file for testing
            val tempFile = File.createTempFile("test", ".jar")
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use {
                it.write("dummy content".toByteArray())
            }

            var createVersionCalled = false
            val client =
                createMockClient { request ->
                    when {
                        request.url.encodedPath == "/version" -> {
                            createVersionCalled = true
                            val responseProject =
                                ModrinthVersion(
                                    id = "new-version-id",
                                    versionNumber = "1.0.0",
                                    gameVersions = listOf("1.20"),
                                    loaders = listOf("fabric"),
                                    files = emptyList(),
                                )
                            request.respond(
                                json.encodeToString(responseProject),
                                status = HttpStatusCode.OK,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            request.respond("", HttpStatusCode.OK)
                        }
                    }
                }

            val modrinthClient = ModrinthClient(token = "test-token", baseUrl = "http://localhost", client = client)
            val registryProvider = ModrinthRegistryProvider(modrinthClient)

            val localProject =
                TerracottaProject(
                    id = "my-mod",
                    name = "My Mod",
                    summary = "",
                    description = "",
                    versions =
                        listOf(
                            TerracottaVersion(
                                "1.0.0",
                                tempFile.absolutePath,
                                listOf("1.20"),
                                listOf("fabric"),
                            ),
                        ),
                    tags = emptyList(),
                    license = "MIT",
                )

            registryProvider.apply("my-mod", emptyList())
        }
}
