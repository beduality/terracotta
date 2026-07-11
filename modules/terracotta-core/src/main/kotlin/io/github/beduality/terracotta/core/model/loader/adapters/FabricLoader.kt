package io.github.beduality.terracotta.core.model.loader

import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Built-in [TerracottaLoader] for Fabric.
 *
 * @see [Loaders reference](https://beduality.github.io/terracotta/content/modules/core/reference/loaders.html)
 * @see [Add a new loader guide](https://beduality.github.io/terracotta/content/modules/core/how-to-guides/add-a-new-loader.html)
 * @see [Loader hierarchy explanation](https://beduality.github.io/terracotta/content/modules/core/explanation/loader-hierarchy.html)
 */

open class FabricLoader : AbstractTerracottaLoader("fabric", "Fabric") {
    /** Returns `true` if this loader is present in [cache]. */
    override fun detect(cache: ProjectFileCache): Boolean = cache.read("src/main/resources/fabric.mod.json") != null

    /** Detects Environment from project files. */
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

    /** Detects GameVersions from project files. */
    override fun detectGameVersions(cache: ProjectFileCache): List<String> {
        val content = cache.read("src/main/resources/fabric.mod.json") ?: return emptyList()
        val json = Json.parseToJsonElement(content) as? JsonObject ?: return emptyList()
        val depends = json["depends"] as? JsonObject ?: return emptyList()
        val minecraft = depends["minecraft"] ?: return emptyList()
        return listOfNotNull(minecraft.jsonPrimitive.content)
    }
}
