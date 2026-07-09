package io.github.beduality.terracotta.github

import com.pulumi.Pulumi
import com.pulumi.github.ActionsSecret
import com.pulumi.github.ActionsSecretArgs
import com.pulumi.github.Repository
import com.pulumi.github.RepositoryArgs

fun main(args: Array<String>) {
    Pulumi.run { ctx ->
        val config = ctx.config()

        // Define the GitHub repository
        val repository =
            Repository(
                "terracotta",
                RepositoryArgs.builder()
                    .name("terracotta")
                    .description("Declarative Minecraft project registry management tool written in Kotlin.")
                    .topics(
                        "minecraft",
                        "modrinth",
                        "registry",
                        "deployment",
                        "metadata",
                        "kotlin",
                        "terracotta",
                        "cli",
                    )
                    .visibility("public")
                    .hasIssues(true)
                    .hasWiki(false)
                    .hasProjects(false)
                    .build(),
                null,
            )

        // Define GitHub Actions secrets if they are set in the configuration
        val secrets =
            listOf(
                "OSSRH_USERNAME",
                "OSSRH_PASSWORD",
                "SIGNING_KEY",
                "SIGNING_PASSWORD",
            )

        for (secretName in secrets) {
            // Convert to camelCase (e.g., OSSRH_USERNAME -> ossrhUsername)
            val configKey =
                secretName.lowercase().split("_")
                    .mapIndexed { index, part ->
                        if (index == 0) part else part.replaceFirstChar { it.uppercase() }
                    }.joinToString("")

            val secretValue = config.get(configKey).orElse(null)
            if (secretValue != null) {
                ActionsSecret(
                    secretName,
                    ActionsSecretArgs.builder()
                        .repository(repository.name())
                        .secretName(secretName)
                        .value(config.requireSecret(configKey))
                        .build(),
                    null,
                )
            }
        }
    }
}
