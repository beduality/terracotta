package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class QuiltLoader : AbstractTerracottaLoader("quilt", "Quilt", FabricLoader()) {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/quilt.mod.json") != null

    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/quilt.mod.json") ?: return emptyList()
        val json = Json.parseToJsonElement(content) as? JsonObject ?: return emptyList()
        val depends = json["depends"] as? JsonObject ?: return emptyList()
        val minecraft = depends["minecraft"] ?: return emptyList()
        return listOfNotNull(minecraft.jsonPrimitive.content)
    }
}
