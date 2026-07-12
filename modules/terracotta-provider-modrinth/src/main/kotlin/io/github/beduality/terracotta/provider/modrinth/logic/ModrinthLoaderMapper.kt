package io.github.beduality.terracotta.provider.modrinth.logic

import io.github.beduality.terracotta.core.provider.logic.LoaderMapper

/**
 * Identity loader mapper for Modrinth.
 *
 * Modrinth exposes loader slugs directly, so the canonical loader ID is used
 * as the platform loader name without transformation.
 */
object ModrinthLoaderMapper : LoaderMapper {
    override fun mapToPlatform(loaderId: String): String? = loaderId
}
