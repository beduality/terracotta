package io.github.beduality.terracotta.core.model

data class TerracottaProject(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val versions: List<TerracottaVersion>,
    val tags: List<String>,
    val license: String,
)
