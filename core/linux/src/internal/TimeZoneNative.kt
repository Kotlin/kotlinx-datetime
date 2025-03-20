/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone

internal actual fun timeZoneById(zoneId: String): TimeZone =
    RegionTimeZone(tzdb.getOrThrow().rulesForId(zoneId), zoneId)

internal actual fun getAvailableZoneIds(): Set<String> =
    tzdb.getOrThrow().availableTimeZoneIds()

private val tzdb = runCatching { TzdbOnFilesystem() }

@OptIn(ExperimentalForeignApi::class)
private fun getTimezoneFromEtcTimezone(): String? {
    val timezoneContent = Path.fromString("/etc/timezone").readBytes()?.toKString()?.trim() ?: return null
    val zoneId = chaseSymlinks("/usr/share/zoneinfo/$timezoneContent")
        ?.splitTimeZonePath()?.second?.toString()
        ?: return null

    val zoneInfoFile = Path.fromString("/usr/share/zoneinfo/$zoneId").readBytes() ?: return null
    val etcLocaltime = Path.fromString("/etc/localtime").readBytes() ?: return null

    if (!etcLocaltime.contentEquals(zoneInfoFile)) {
        throw IllegalTimeZoneException(
            "Timezone mismatch: /etc/timezone specifies " +
                    "'${if (timezoneContent != zoneId) timezoneContent else zoneId}' " +
                    "but /etc/localtime content differs from /usr/share/zoneinfo/$zoneId"
        )
    }

    return zoneId
}

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> {
    val zonePath = currentSystemTimeZonePath ?: return "Z" to null
    zonePath.splitTimeZonePath()?.second?.toString()?.let { zoneId ->
        return zoneId to null
    }

    getTimezoneFromEtcTimezone()?.let { zoneId ->
        return zoneId to null
    }

    throw IllegalTimeZoneException("Could not determine the timezone ID that `$zonePath` corresponds to")
}