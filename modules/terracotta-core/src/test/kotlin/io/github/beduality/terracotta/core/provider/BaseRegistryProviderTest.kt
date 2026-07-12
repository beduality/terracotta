package io.github.beduality.terracotta.core.provider

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.model.releasetype.TerracottaReleaseType
import io.github.beduality.terracotta.core.model.version.TerracottaVersion
import io.github.beduality.terracotta.core.provider.logic.LoaderMapper
import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic
import io.github.beduality.terracotta.core.test.CapturingLogger
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class BaseRegistryProviderTest {
    @Test
    fun `apply filters unsupported operations and logs warnings before delegating`() =
        runTest {
            val capturingLogger = CapturingLogger()
            val behavior =
                object : PlatformBehavior {
                    override val isStateful: Boolean = false
                }
            val providerLogic =
                object : ProviderLogic {
                    override val loaderMapper: LoaderMapper =
                        object : LoaderMapper {
                            override fun mapToPlatform(loaderId: String): String = loaderId
                        }
                    override val platformBehavior: PlatformBehavior = behavior
                }
            val version =
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = "",
                    gameVersions = emptyList(),
                    releaseType = TerracottaReleaseType.RELEASE,
                )
            val description = Operation.UpdateDescription("old", "new")
            val upload = Operation.UploadVersion(version)
            val operations = listOf(description, upload)
            val provider = TestRegistryProvider(providerLogic, capturingLogger)

            provider.apply("project", operations)

            assertEquals(listOf(upload), provider.appliedOperations)
            assertEquals(1, capturingLogger.warnings.size)
            assertTrue(capturingLogger.warnings.first().contains("Platform 'test' does not support operation"))
        }

    @Test
    fun `apply delegates all operations when platform is stateful`() =
        runTest {
            val capturingLogger = CapturingLogger()
            val behavior =
                object : PlatformBehavior {
                    override val isStateful: Boolean = true
                }
            val providerLogic =
                object : ProviderLogic {
                    override val loaderMapper: LoaderMapper =
                        object : LoaderMapper {
                            override fun mapToPlatform(loaderId: String): String = loaderId
                        }
                    override val platformBehavior: PlatformBehavior = behavior
                }
            val description = Operation.UpdateDescription("old", "new")
            val provider = TestRegistryProvider(providerLogic, capturingLogger)

            provider.apply("project", listOf(description))

            assertEquals(listOf(description), provider.appliedOperations)
            assertTrue(capturingLogger.warnings.isEmpty())
        }

    private class TestRegistryProvider(
        providerLogic: ProviderLogic,
        private val capturingLogger: CapturingLogger,
    ) : BaseRegistryProvider(providerLogic, "test") {
        val appliedOperations = mutableListOf<Operation>()

        override val logger: Logger get() = capturingLogger

        override suspend fun applySupported(
            projectId: String,
            operations: List<Operation>,
        ) {
            appliedOperations.addAll(operations)
        }
    }
}
