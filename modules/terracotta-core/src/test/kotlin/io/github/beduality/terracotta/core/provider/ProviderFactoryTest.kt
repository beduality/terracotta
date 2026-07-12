package io.github.beduality.terracotta.core.provider

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderFactoryTest {
    @Test
    fun `createDestructiveRegistryProvider defaults to null`() {
        val factory =
            object : ProviderFactory {
                override val id: String = "test"

                override fun createStateProvider(token: String?): StateProvider {
                    throw UnsupportedOperationException()
                }

                override fun createRegistryProvider(token: String?): RegistryProvider {
                    throw UnsupportedOperationException()
                }
            }

        assertNull(factory.createDestructiveRegistryProvider("token"))
    }

    @Test
    fun `destructive registry provider can be implemented and invoked`() =
        runTest {
            val deletedProjects = mutableListOf<String>()
            val deletedVersions = mutableListOf<String>()

            val provider =
                object : DestructiveRegistryProvider {
                    override suspend fun deleteProject(projectId: String) {
                        deletedProjects.add(projectId)
                    }

                    override suspend fun deleteAllVersions(projectId: String) {
                        deletedVersions.add(projectId)
                    }
                }

            provider.deleteProject("my-project")
            provider.deleteAllVersions("my-project")

            assertEquals(listOf("my-project"), deletedProjects)
            assertEquals(listOf("my-project"), deletedVersions)
        }

    @Test
    fun `custom factory can return a destructive registry provider`() {
        val provider =
            object : DestructiveRegistryProvider {
                override suspend fun deleteProject(projectId: String) {}

                override suspend fun deleteAllVersions(projectId: String) {}
            }

        val factory =
            object : ProviderFactory {
                override val id: String = "test"

                override fun createStateProvider(token: String?): StateProvider {
                    throw UnsupportedOperationException()
                }

                override fun createRegistryProvider(token: String?): RegistryProvider {
                    throw UnsupportedOperationException()
                }

                override fun createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider = provider
            }

        assertTrue(factory.createDestructiveRegistryProvider(null) === provider)
    }
}
