package io.github.beduality.terracotta.core.model

import io.github.beduality.terracotta.core.model.version.TerracottaVersion

data class TerracottaProject(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val versions: List<TerracottaVersion>,
    val tags: List<String>,
    val license: String,
)
