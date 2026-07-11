package io.github.beduality.terracotta.provider.hangar.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HangarLoaderMapperTest {
    @Test
    fun `maps paper ecosystem loaders to PAPER platform`() {
        assertEquals(setOf("PAPER"), HangarLoaderMapper.mapToPlatforms(listOf("bukkit", "spigot", "paper", "purpur", "folia")))
    }

    @Test
    fun `maps velocity to VELOCITY platform`() {
        assertEquals(setOf("VELOCITY"), HangarLoaderMapper.mapToPlatforms(listOf("velocity")))
    }

    @Test
    fun `maps bungeecord and waterfall to WATERFALL platform`() {
        assertEquals(setOf("WATERFALL"), HangarLoaderMapper.mapToPlatforms(listOf("bungeecord", "waterfall")))
    }

    @Test
    fun `skips unsupported mod loaders`() {
        assertEquals(emptySet<String>(), HangarLoaderMapper.mapToPlatforms(listOf("fabric", "forge", "quilt", "neoforge", "sponge")))
    }

    @Test
    fun `combines supported and unsupported loaders`() {
        assertEquals(setOf("PAPER", "VELOCITY"), HangarLoaderMapper.mapToPlatforms(listOf("paper", "fabric", "velocity", "sponge")))
    }

    @Test
    fun `maps platforms back to canonical loaders`() {
        assertEquals("paper", HangarLoaderMapper.mapPlatformToLoader("PAPER"))
        assertEquals("velocity", HangarLoaderMapper.mapPlatformToLoader("VELOCITY"))
        assertEquals("waterfall", HangarLoaderMapper.mapPlatformToLoader("WATERFALL"))
        assertNull(HangarLoaderMapper.mapPlatformToLoader("FABRIC"))
    }
}
