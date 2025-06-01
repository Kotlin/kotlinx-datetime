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

// This workaround is needed for Debian versions Etch (4.0) - Jessie (8.0), where the timezone data is organized differently.
// See: https://github.com/Kotlin/kotlinx-datetime/issues/430
@OptIn(ExperimentalForeignApi::class)
private fun getTimezoneFromEtcTimezone(): String? {
    val timezoneContent = Path.fromString("${systemTimezoneSearchRoot}etc/timezone").readBytes()?.toKString()?.trim() ?: return null
    val zoneId = chaseSymlinks("${systemTimezoneSearchRoot}usr/share/zoneinfo/$timezoneContent")
        ?.splitTimeZonePath()?.second?.toString()
        ?: return null

    val zoneInfoBytes = Path.fromString("${systemTimezoneSearchRoot}usr/share/zoneinfo/$zoneId").readBytes() ?: return null
    val localtimeBytes = Path.fromString("${systemTimezoneSearchRoot}etc/localtime").readBytes() ?: return null

    if (!localtimeBytes.contentEquals(zoneInfoBytes)) {
        val displayTimezone = when (timezoneContent) {
            zoneId -> "'$zoneId'"
            else -> "'$timezoneContent' (resolved to '$zoneId')"
        }
        throw IllegalTimeZoneException(
            "Timezone mismatch: ${systemTimezoneSearchRoot}etc/timezone specifies $displayTimezone " +
                    "but ${systemTimezoneSearchRoot}etc/localtime content differs from ${systemTimezoneSearchRoot}usr/share/zoneinfo/$zoneId"
        )
    }

    return zoneId
}

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> {
    // According to https://www.man7.org/linux/man-pages/man5/localtime.5.html, UTC is used when /etc/localtime is missing.
    // If /etc/localtime exists but isn't a symlink, we check if it's a copy of a timezone file by examining /etc/timezone
    // (which is a Debian-specific approach used in older distributions).
    val zonePath = currentSystemTimeZonePath ?: return "UTC" to null

    zonePath.splitTimeZonePath()?.second?.toString()?.let { zoneId ->
        return zoneId to null
    }

    getTimezoneFromEtcTimezone()?.let { zoneId ->
        return zoneId to null
    }

    throw IllegalTimeZoneException("Could not determine the timezone ID that `$zonePath` corresponds to")
}