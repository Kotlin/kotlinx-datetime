/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*

internal actual class TimeZoneDatabase {
    actual companion object {
        actual fun rulesForId(id: String): TimeZoneRules = tzdbOnFilesystem.rulesForId(id)

        actual fun currentSystemDefault(): Pair<String, TimeZoneRules?> {
            val zoneId = tzdbOnFilesystem.currentSystemDefault()?.second?.toString()
                ?: throw IllegalStateException("Failed to get the system timezone")
            return zoneId to null
        }

        actual val availableZoneIds: Set<String>
            get() = tzdbOnFilesystem.availableTimeZoneIds()
    }
}

@OptIn(UnsafeNumber::class)
internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME, tm.ptr)
    if (error != 0) {
        val errorStr: String = strerror(errno)?.toKString() ?: "Unknown error"
        throw IllegalStateException("Could not obtain the current clock readings from the system: $errorStr")
    }
    val seconds: Long = tm.tv_sec.convert<Long>()
    val nanoseconds: Int = tm.tv_nsec.convert<Int>()
    try {
        require(nanoseconds in 0 until NANOS_PER_ONE)
        return Instant(seconds, nanoseconds)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock are not representable as an Instant")
    }
}

private val tzdbOnFilesystem = TzdbOnFilesystem(Path.fromString("/usr/share/zoneinfo"))
