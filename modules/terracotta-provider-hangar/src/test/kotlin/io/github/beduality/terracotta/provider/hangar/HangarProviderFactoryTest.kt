package io.github.beduality.terracotta.provider.hangar

import io.github.beduality.terracotta.core.provider.ProviderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

class HangarProviderFactoryTest {
    @Test
    fun `HangarProviderFactory is registered via ServiceLoader`() {
        val loader = ServiceLoader.load(ProviderFactory::class.java)
        val factory = loader.find { it.id == "hangar" }
        assertNotNull(factory)
        assertEquals("hangar", factory?.id)
    }

    @Test
    fun `HangarProviderFactory creates providers`() {
        val factory = HangarProviderFactory()
        assertNotNull(factory.createStateProvider(null))
        assertNotNull(factory.createRegistryProvider("api-key"))
    }
}
