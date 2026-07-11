package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

open class FabricLoader : AbstractTerracottaLoader("fabric", "Fabric") {
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/fabric.mod.json") != null

    override fun detectEnvironment(cache: ProjectFileCache): TerracottaEnvironment? {
        val content = cache.read("src/main/resources/fabric.mod.json") ?: return null
        val json = Json.parseToJsonElement(content) as? JsonObject ?: return null
        return when (json["environment"]?.jsonPrimitive?.content?.lowercase()) {
            "client" -> TerracottaEnvironment.CLIENT_ONLY
            "server" -> TerracottaEnvironment.SERVER_ONLY
            "*" -> TerracottaEnvironment.UNIVERSAL
            else -> null
        }
    }
}
