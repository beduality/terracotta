package io.github.beduality.terracotta.core.provider.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoaderMapperTest {
    @Test
    fun `mapToPlatforms skips unsupported loaders and removes duplicates`() {
        val mapper =
            object : LoaderMapper {
                override fun mapToPlatform(loaderId: String): String? =
                    when (loaderId) {
                        "a", "b" -> "AB"
                        "c" -> "C"
                        else -> null
                    }
            }

        assertEquals(
            setOf("AB", "C"),
            mapper.mapToPlatforms(listOf("a", "b", "c", "d", "a")),
        )
    }

    @Test
    fun `mapToPlatforms returns empty set when all loaders are unsupported`() {
        val mapper =
            object : LoaderMapper {
                override fun mapToPlatform(loaderId: String): String? = null
            }

        assertEquals(emptySet<String>(), mapper.mapToPlatforms(listOf("a", "b")))
    }

    @Test
    fun `mapToPlatforms returns empty set for empty input`() {
        val mapper =
            object : LoaderMapper {
                override fun mapToPlatform(loaderId: String): String? = loaderId
            }

        assertEquals(emptySet<String>(), mapper.mapToPlatforms(emptyList()))
    }

    @Test
    fun `identity mapper returns loader id unchanged`() {
        val mapper =
            object : LoaderMapper {
                override fun mapToPlatform(loaderId: String): String? = loaderId
            }

        assertEquals("fabric", mapper.mapToPlatform("fabric"))
        assertEquals("", mapper.mapToPlatform(""))
    }
}
