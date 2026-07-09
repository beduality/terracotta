package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModrinthProviderTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ModrinthClient
    private lateinit var stateProvider: ModrinthStateProvider
    private lateinit var registryProvider: ModrinthRegistryProvider

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/v2").toString()
        client = ModrinthClient("fake-token", baseUrl)
        stateProvider = ModrinthStateProvider(client)
        registryProvider = ModrinthRegistryProvider(client)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `test fetchProject returns null on 404`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val project = stateProvider.fetchProject("non-existent")

        assertNull(project)
    }

    @Test
    fun `test fetchProject parses successfully`() {
        val projectJson =
            """
            {
                "id": "my-plugin",
                "slug": "my-plugin",
                "title": "My Plugin",
                "summary": "Plugin summary",
                "body": "Plugin description",
                "categories": ["paper", "utility"],
                "license": {
                    "id": "MIT"
                }
            }
            """.trimIndent()

        val versionsJson =
            """
            [
                {
                    "version_number": "1.0.0",
                    "game_versions": ["1.20"],
                    "loaders": ["paper"],
                    "files": [
                        {
                            "url": "https://example.com/file.jar",
                            "filename": "my-plugin.jar",
                            "primary": true
                        }
                    ]
                }
            ]
            """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(projectJson))
        server.enqueue(MockResponse().setResponseCode(200).setBody(versionsJson))

        val project = stateProvider.fetchProject("my-plugin")

        assertNotNull(project)
        assertEquals("my-plugin", project?.id)
        assertEquals("My Plugin", project?.name)
        assertEquals("MIT", project?.license)
        assertEquals(1, project?.versions?.size)
        assertEquals("1.0.0", project?.versions?.first()?.version)
        assertEquals(listOf("paper"), project?.versions?.first()?.loaders)
    }

    @Test
    fun `test apply updates metadata on remote registry`() {
        server.enqueue(MockResponse().setResponseCode(200)) // patch response

        val ops =
            listOf(
                Operation.UpdateMetadata(
                    nameChanged = true,
                    summaryChanged = true,
                    licenseChanged = true,
                    newName = "New Name",
                    newSummary = "New Summary",
                    newLicense = "Apache-2.0",
                ),
            )

        registryProvider.apply("my-plugin", ops)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/v2/project/my-plugin", request.path)
        assertEquals("fake-token", request.getHeader("Authorization"))

        val requestBody = request.body.readUtf8()
        assertEquals("""{"title":"New Name","summary":"New Summary","license_id":"Apache-2.0"}""", requestBody)
    }
}
