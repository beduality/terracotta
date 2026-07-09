package io.github.beduality.terracotta.cli

import io.github.beduality.terracotta.cli.command.ApplyCommand
import io.github.beduality.terracotta.cli.command.PlanCommand
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "terracotta",
    mixinStandardHelpOptions = true,
    version = ["1.0.0"],
    description = ["Declarative plugin registry management tool."],
    subcommands = [PlanCommand::class, ApplyCommand::class],
)
class TerracottaCommand : Callable<Int> {
    override fun call(): Int {
        CommandLine.usage(this, System.out)
        return 0
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(TerracottaCommand()).execute(*args)
    exitProcess(exitCode)
}
