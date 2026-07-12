package io.github.beduality.terracotta.core.test

import org.slf4j.Logger
import org.slf4j.Marker

/**
 * Test double that captures all `warn(String)` messages.
 *
 * All other SLF4J methods are no-ops.
 */
class CapturingLogger : Logger {
    val warnings = mutableListOf<String>()

    override fun warn(msg: String?) {
        if (msg != null) warnings.add(msg)
    }

    override fun getName(): String = "CapturingLogger"

    override fun isTraceEnabled(): Boolean = false

    override fun isTraceEnabled(marker: Marker?): Boolean = false

    override fun trace(msg: String?) = Unit

    override fun trace(
        format: String?,
        arg: Any?,
    ) = Unit

    override fun trace(
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun trace(
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun trace(
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun trace(
        marker: Marker?,
        msg: String?,
    ) = Unit

    override fun trace(
        marker: Marker?,
        format: String?,
        arg: Any?,
    ) = Unit

    override fun trace(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun trace(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun trace(
        marker: Marker?,
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun isDebugEnabled(): Boolean = false

    override fun isDebugEnabled(marker: Marker?): Boolean = false

    override fun debug(msg: String?) = Unit

    override fun debug(
        format: String?,
        arg: Any?,
    ) = Unit

    override fun debug(
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun debug(
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun debug(
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun debug(
        marker: Marker?,
        msg: String?,
    ) = Unit

    override fun debug(
        marker: Marker?,
        format: String?,
        arg: Any?,
    ) = Unit

    override fun debug(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun debug(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun debug(
        marker: Marker?,
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun isInfoEnabled(): Boolean = false

    override fun isInfoEnabled(marker: Marker?): Boolean = false

    override fun info(msg: String?) = Unit

    override fun info(
        format: String?,
        arg: Any?,
    ) = Unit

    override fun info(
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun info(
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun info(
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun info(
        marker: Marker?,
        msg: String?,
    ) = Unit

    override fun info(
        marker: Marker?,
        format: String?,
        arg: Any?,
    ) = Unit

    override fun info(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun info(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun info(
        marker: Marker?,
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun isWarnEnabled(): Boolean = true

    override fun isWarnEnabled(marker: Marker?): Boolean = true

    override fun warn(
        format: String?,
        arg: Any?,
    ) = Unit

    override fun warn(
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun warn(
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun warn(
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun warn(
        marker: Marker?,
        msg: String?,
    ) = Unit

    override fun warn(
        marker: Marker?,
        format: String?,
        arg: Any?,
    ) = Unit

    override fun warn(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun warn(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun warn(
        marker: Marker?,
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun isErrorEnabled(): Boolean = false

    override fun isErrorEnabled(marker: Marker?): Boolean = false

    override fun error(msg: String?) = Unit

    override fun error(
        format: String?,
        arg: Any?,
    ) = Unit

    override fun error(
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun error(
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun error(
        msg: String?,
        t: Throwable?,
    ) = Unit

    override fun error(
        marker: Marker?,
        msg: String?,
    ) = Unit

    override fun error(
        marker: Marker?,
        format: String?,
        arg: Any?,
    ) = Unit

    override fun error(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?,
    ) = Unit

    override fun error(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?,
    ) = Unit

    override fun error(
        marker: Marker?,
        msg: String?,
        t: Throwable?,
    ) = Unit
}
