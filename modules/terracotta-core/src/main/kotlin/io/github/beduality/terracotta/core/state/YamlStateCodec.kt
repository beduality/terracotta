package io.github.beduality.terracotta.core.state

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * Encodes and decodes [TerracottaState] snapshots to and from YAML.
 *
 * This codec is intentionally internal to `terracotta-core`; the public API for
 * persistence is [StateSource].
 */
internal object YamlStateCodec {
    fun encode(state: TerracottaState): String {
        val map = mutableMapOf<String, Any?>()
        map["version"] = state.version
        state.lastRun?.let { map["lastRun"] = encodeRunSummary(it) }
        state.projectId?.let { map["projectId"] = it }
        if (state.providers.isNotEmpty()) {
            map["providers"] = state.providers.mapValues { (_, provider) -> encodeProviderState(provider) }
        }
        return Yaml().dump(map)
    }

    fun decode(yaml: String): TerracottaState {
        return try {
            val map = Yaml().load(yaml) as? Map<String, Any?> ?: throw IOException("State file is not a YAML map")
            decodeState(map)
        } catch (e: YAMLException) {
            throw IOException("Failed to parse state file: ${e.message}", e)
        }
    }

    private fun encodeRunSummary(summary: RunSummary): Map<String, Any?> {
        val map =
            mutableMapOf<String, Any?>(
                "command" to summary.command,
                "startedAt" to summary.startedAt.toString(),
            )
        summary.finishedAt?.let { map["finishedAt"] = it.toString() }
        summary.commitSha?.let { map["commitSha"] = it }
        return map
    }

    private fun encodeProviderState(provider: ProviderState): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (provider.versionIds.isNotEmpty()) {
            map["versionIds"] = provider.versionIds
        }
        if (provider.gallery.isNotEmpty()) {
            map["gallery"] = provider.gallery.mapValues { (_, identity) -> encodeGalleryItemIdentity(identity) }
        }
        provider.metadataHash?.let { map["metadataHash"] = it }
        return map
    }

    private fun encodeGalleryItemIdentity(identity: GalleryItemIdentity): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>("localKey" to identity.localKey)
        identity.remoteUrl?.let { map["remoteUrl"] = it }
        identity.remoteId?.let { map["remoteId"] = it }
        return map
    }

    private fun decodeState(map: Map<String, Any?>): TerracottaState {
        return TerracottaState(
            version = map.readInt("version") ?: 1,
            lastRun = map["lastRun"]?.let { decodeRunSummary(it) },
            projectId = map.readString("projectId"),
            providers = decodeProviders(map["providers"]),
        )
    }

    private fun decodeRunSummary(value: Any?): RunSummary {
        @Suppress("UNCHECKED_CAST")
        val map = value as? Map<String, Any?> ?: throw IOException("lastRun is not a map")
        return RunSummary(
            command = map.readString("command") ?: throw IOException("lastRun.command is missing"),
            startedAt = map.readInstant("startedAt") ?: throw IOException("lastRun.startedAt is missing or invalid"),
            finishedAt = map.readInstant("finishedAt"),
            commitSha = map.readString("commitSha"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeProviders(value: Any?): Map<String, ProviderState> {
        val map = value as? Map<String, Map<String, Any?>> ?: return emptyMap()
        return map.mapValues { (_, providerMap) -> decodeProviderState(providerMap) }
    }

    private fun decodeProviderState(map: Map<String, Any?>): ProviderState {
        @Suppress("UNCHECKED_CAST")
        val galleryMap = map["gallery"] as? Map<String, Map<String, Any?>>
        return ProviderState(
            versionIds = map.readStringList("versionIds") ?: emptyList(),
            gallery = galleryMap?.mapValues { (_, identityMap) -> decodeGalleryItemIdentity(identityMap) } ?: emptyMap(),
            metadataHash = map.readString("metadataHash"),
        )
    }

    private fun decodeGalleryItemIdentity(map: Map<String, Any?>): GalleryItemIdentity {
        return GalleryItemIdentity(
            localKey = map.readString("localKey") ?: throw IOException("gallery item localKey is missing"),
            remoteUrl = map.readString("remoteUrl"),
            remoteId = map.readString("remoteId"),
        )
    }

    private fun Map<String, Any?>.readString(key: String): String? = this[key]?.toString()

    private fun Map<String, Any?>.readInt(key: String): Int? {
        return when (val value = this[key]) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.readStringList(key: String): List<String>? {
        val value = this[key] ?: return null
        return if (value is List<*>) value.map { it.toString() } else null
    }

    private fun Map<String, Any?>.readInstant(key: String): Instant? {
        val value = this[key] ?: return null
        return when (value) {
            is Instant -> value
            is Date -> value.toInstant()
            else -> {
                try {
                    Instant.parse(value.toString())
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }
}
