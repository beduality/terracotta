package io.github.beduality.terracotta.provider.hangar

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
}
