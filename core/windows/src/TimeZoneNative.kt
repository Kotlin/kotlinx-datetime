/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime

import kotlinx.cinterop.*
import kotlinx.datetime.internal.*
import platform.posix.*

internal actual class TimeZoneDatabase {
    actual companion object {
        actual fun rulesForId(id: String): TimeZoneRules = tzdbInRegistry.rulesForId(id)

        actual fun currentSystemDefault(): Pair<String, TimeZoneRules?> = tzdbInRegistry.currentSystemDefault()

        actual val availableZoneIds: Set<String>
            get() = tzdbInRegistry.availableTimeZoneIds()
    }
}

private val tzdbInRegistry = TzdbInRegistry()

internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME, tm.ptr)
    check(error == 0) { "Error when reading the system clock: ${strerror(errno)}" }
    try {
        require(tm.tv_nsec in 0 until NANOS_PER_ONE)
        Instant(tm.tv_sec, tm.tv_nsec)
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock (${tm.tv_sec} seconds, ${tm.tv_nsec} nanoseconds) are not representable as an Instant")
    }
}
