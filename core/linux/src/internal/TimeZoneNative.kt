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
    return chaseSymlinks("/usr/share/zoneinfo/$timezoneContent")?.splitTimeZonePath()?.second?.toString()
}

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> {
    val zonePath = currentSystemTimeZonePath
    if (zonePath != null) {
        var zoneId = zonePath.splitTimeZonePath()?.second?.toString()
        if (zoneId != null) {
            return zoneId to null
        } else {
            zoneId = getTimezoneFromEtcTimezone()
            if (zoneId != null) {
                return zoneId to null
            } else {
                throw IllegalTimeZoneException("Could not determine the timezone ID that `$zonePath` corresponds to")
            }
        }
    }

    return "Z" to null
}
