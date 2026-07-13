package io.github.beduality.terracotta.provider.modrinth.logic

import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior

/**
 * Platform behavior for Modrinth.
 *
 * Modrinth is a stateful registry: it supports project metadata updates,
 * description changes, categories, version uploads, gallery images, and project icon
 * uploads. All operations are passed through unchanged.
 */
object ModrinthPlatformBehavior : PlatformBehavior {
    override val isStateful: Boolean = true
}
