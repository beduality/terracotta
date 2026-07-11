package io.github.beduality.terracotta.core.detect.adapters

import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

object BuildFileGameVersionSource {
    fun extract(cache: ProjectFileCache): List<String> {
        val candidates = mutableListOf<String>()

        cache.read("gradle.properties")?.let { candidates += extractFromProperties(it) }
        cache.read("gradle/libs.versions.toml")?.let { candidates += extractFromVersionCatalog(it) }
        cache.read("build.gradle.kts")?.let { candidates += extractFromBuildScript(it) }
        cache.read("build.gradle")?.let { candidates += extractFromBuildScript(it) }

        return candidates
    }

    private fun extractFromProperties(content: String): List<String> {
        val keys =
            listOf(
                "minecraft_version",
                "minecraftVersion",
                "mcVersion",
                "paper_version",
                "spigot_version",
                "bukkit_version",
                "folia_version",
                "purpur_version",
            )
        val regex = Regex("""(?:^|\n)\s*(?:${keys.joinToString("|")})\s*=\s*([^\s]+)""", RegexOption.MULTILINE)
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }

    private fun extractFromVersionCatalog(content: String): List<String> {
        val keys = listOf("minecraft", "minecraftVersion", "mcVersion")
        val regex = Regex("""(?:^|\n)\s*(?:${keys.joinToString("|")})\s*=\s*"([^"]+)""", RegexOption.MULTILINE)
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }

    private fun extractFromBuildScript(content: String): List<String> {
        val candidates = mutableListOf<String>()

        val apiCoordinates =
            listOf(
                "paper-api",
                "spigot-api",
                "bukkit-api",
                "folia-api",
                "purpur-api",
                "velocity-api",
                "bungeecord-api",
                "waterfall-api",
            )
        val apiRegex =
            Regex(
                """(?:${apiCoordinates.joinToString("|")})\s*(?:\(|=)\s*["']([^"'\)]+)["']""",
                RegexOption.MULTILINE,
            )
        candidates += apiRegex.findAll(content).map { it.groupValues[1] }

        val mavenRegex =
            Regex(
                """(?:io\.papermc\.paper:paper-api|org\.spigotmc:spigot-api|org\.bukkit:bukkit-api|io\.papermc\.folia:folia-api|io\.papermc\.purpur:purpur-api|com\.velocitypowered:velocity-api|com\.mojang:minecraft|net\.minecraftforge:forge):([^:"\s]+)""",
                RegexOption.MULTILINE,
            )
        candidates += mavenRegex.findAll(content).map { it.groupValues[1] }

        val minecraftCallRegex =
            Regex(
                """minecraft\s*(?:\(|=)\s*["']([^"'\)]+)["']""",
                RegexOption.MULTILINE,
            )
        candidates += minecraftCallRegex.findAll(content).map { it.groupValues[1] }

        return candidates
    }
}
