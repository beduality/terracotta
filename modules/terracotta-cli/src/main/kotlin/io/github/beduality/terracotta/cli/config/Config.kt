package io.github.beduality.terracotta.cli.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaVersion
import java.io.File

data class YamlProjectSection(
    val id: String = "",
    val name: String = "",
    val summary: String = "",
)

data class YamlVersion(
    val version: String = "",
    val artifact: String = "",
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
)

data class TerracottaConfig(
    val project: YamlProjectSection = YamlProjectSection(),
    val description: String = "",
    val versions: List<YamlVersion> = emptyList(),
    val tags: List<String> = emptyList(),
    val license: String = "",
)

fun parseConfig(file: File): TerracottaProject {
    if (!file.exists()) {
        throw IllegalArgumentException("Configuration file not found: ${file.absolutePath}")
    }
    val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    val config: TerracottaConfig = mapper.readValue(file)

    // Load description body from external file if specified
    val descriptionContent =
        if (config.description.endsWith(".md")) {
            val descFile = File(file.parentFile ?: File("."), config.description)
            if (descFile.exists()) descFile.readText() else config.description
        } else {
            config.description
        }

    return TerracottaProject(
        id = config.project.id,
        name = config.project.name,
        summary = config.project.summary,
        description = descriptionContent,
        versions =
            config.versions.map {
                // Check if version artifact file path is relative to config file directory
                val absoluteArtifactPath = File(file.parentFile ?: File("."), it.artifact).absolutePath
                TerracottaVersion(
                    version = it.version,
                    artifactPath = absoluteArtifactPath,
                    gameVersions = it.gameVersions,
                    loaders = it.loaders,
                )
            },
        tags = config.tags,
        license = config.license,
    )
}
