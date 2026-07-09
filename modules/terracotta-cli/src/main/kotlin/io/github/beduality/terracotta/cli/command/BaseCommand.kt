package io.github.beduality.terracotta.cli.command

import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import picocli.CommandLine.Option
import java.util.concurrent.Callable

abstract class BaseCommand : Callable<Int> {
    @Option(
        names = ["-f", "--file"],
        description = ["Path to the configuration file (default: terracotta.yaml)"],
        defaultValue = "terracotta.yaml",
    )
    var configFile: String = "terracotta.yaml"

    @Option(
        names = ["--modrinth-token"],
        description = ["Modrinth API Token (fallback: MODRINTH_TOKEN env var)"],
        interactive = false,
    )
    var token: String? = null

    protected fun getEffectiveToken(): String? {
        return token ?: System.getenv("MODRINTH_TOKEN")
    }

    protected fun buildClient(): ModrinthClient {
        return ModrinthClient(getEffectiveToken())
    }
}
