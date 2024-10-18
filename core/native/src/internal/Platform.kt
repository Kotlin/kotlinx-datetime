/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.Instant
import platform.posix.*

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun currentTime(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME.convert(), tm.ptr)
    check(error == 0) { "Error when reading the system clock: ${strerror(errno)?.toKString() ?: "Unknown error"}" }
    try {
        require(tm.tv_nsec in 0 until NANOS_PER_ONE)
        Instant(tm.tv_sec.convert(), tm.tv_nsec.convert())
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock (${tm.tv_sec} seconds, ${tm.tv_nsec} nanoseconds) are not representable as an Instant")
    }
}