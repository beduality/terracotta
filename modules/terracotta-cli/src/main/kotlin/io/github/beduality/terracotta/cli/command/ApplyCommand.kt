package io.github.beduality.terracotta.cli.command

import io.github.beduality.terracotta.cli.config.parseConfig
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.provider.modrinth.ModrinthRegistryProvider
import io.github.beduality.terracotta.provider.modrinth.ModrinthStateProvider
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import java.io.File

@Command(
    name = "apply",
    mixinStandardHelpOptions = true,
    description = ["Apply changes to match the configuration."],
)
class ApplyCommand : BaseCommand() {
    override fun call(): Int =
        runBlocking {
            val configPath = File(configFile)
            println("Loading local state from ${configPath.name}...")
            val localProject = parseConfig(configPath)

            val client = buildClient()
            println("Fetching current remote state from Modrinth (Project ID: ${localProject.id})...")
            val stateProvider = ModrinthStateProvider(client)
            val remoteProject = stateProvider.fetchProject(localProject.id)

            val operations = DiffEngine.diff(localProject, remoteProject)
            if (operations.isEmpty()) {
                println("\u001B[32mNo changes needed. Remote project already matches desired state.\u001B[0m")
                return@runBlocking 0
            }

            println("\nPlan:")
            operations.forEach { op ->
                val color =
                    when (op) {
                        is Operation.CreateProject, is Operation.UploadVersion -> "\u001B[32m" // Green  (+)
                        is Operation.UpdateMetadata, is Operation.UpdateDescription, is Operation.UpdateTags -> "\u001B[34m" // Blue (~)
                    }
                println("$color  ${op.description}\u001B[0m")
            }

            val effectiveToken = getEffectiveToken()
            if (effectiveToken.isNullOrBlank()) {
                System.err.println("\u001B[31mError: Modrinth authentication token is required to apply changes.\u001B[0m")
                System.err.println("Please set the MODRINTH_TOKEN environment variable or pass --modrinth-token.")
                return@runBlocking 1
            }

            println("\nApplying changes...")
            val registryProvider = ModrinthRegistryProvider(client)
            // Use the real base62 project ID from the remote state if it exists,
            // otherwise fall back to the local slug (CreateProject will update it)
            val resolvedProjectId = remoteProject?.id ?: localProject.id
            registryProvider.apply(resolvedProjectId, operations)

            println("\u001B[32mSuccess! Terracotta apply complete.\u001B[0m")
            return@runBlocking 0
        }
}
