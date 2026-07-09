package io.github.beduality.terracotta.cli.command

import io.github.beduality.terracotta.cli.config.parseConfig
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.provider.modrinth.ModrinthStateProvider
import kotlinx.coroutines.runBlocking
import picocli.CommandLine.Command
import java.io.File

@Command(
    name = "plan",
    mixinStandardHelpOptions = true,
    description = ["Generate and show an execution plan."],
)
class PlanCommand : BaseCommand() {
    override fun call(): Int =
        runBlocking {
            val configPath = File(configFile)
            println("Loading local state from ${configPath.name}...")
            val localProject = parseConfig(configPath)

            val client = buildClient()
            println("Fetching current remote state from Modrinth (Project ID: ${localProject.id})...")
            val stateProvider = ModrinthStateProvider(client)
            val remoteProject = stateProvider.fetchProject(localProject.id)

            if (remoteProject == null) {
                println("\u001B[33mProject '${localProject.id}' does not exist on Modrinth. Terracotta will create it.\u001B[0m")
            }

            val operations = DiffEngine.diff(localProject, remoteProject)
            if (operations.isEmpty()) {
                println("\u001B[32mNo changes. Remote project matches desired state.\u001B[0m")
                return@runBlocking 0
            }

            println("\nTerracotta will perform the following actions:")
            operations.forEach { op ->
                val color =
                    when (op) {
                        is Operation.UploadVersion -> "\u001B[32m" // Green
                        else -> "\u001B[34m" // Blue
                    }
                println("$color  ${op.description}\u001B[0m")
            }

            println("\nPlan: ${operations.size} to apply.")
            return@runBlocking 0
        }
}
