package io.github.beduality.terracotta.provider.modrinth

import io.github.beduality.terracotta.core.provider.ProviderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

class ModrinthProviderFactoryTest {
    @Test
    fun `ModrinthProviderFactory is registered via ServiceLoader`() {
        val loader = ServiceLoader.load(ProviderFactory::class.java)
        val factory = loader.find { it.id == "modrinth" }
        assertNotNull(factory)
        assertEquals("modrinth", factory?.id)
    }

    @Test
    fun `ModrinthProviderFactory creates providers`() {
        val factory = ModrinthProviderFactory()
        assertNotNull(factory.createStateProvider(null))
        assertNotNull(factory.createRegistryProvider("token"))
    }
}
