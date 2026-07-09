package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ModrinthProviderTest {
    @Test
    fun `test client instantiation`() =
        runTest {
            val client = ModrinthClient("fake-token")
            val stateProvider = ModrinthStateProvider(client)
            val registryProvider = ModrinthRegistryProvider(client)

            assertNotNull(stateProvider)
            assertNotNull(registryProvider)
        }
}
