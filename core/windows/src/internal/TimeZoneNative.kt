/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.*
import platform.posix.*

internal actual val systemTzdb: TimeZoneDatabase get() = tzdbInRegistry

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZoneRules?> = tzdbInRegistry.currentSystemDefault()

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

private val tzdbInRegistry = TzdbInRegistry()
