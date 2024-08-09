/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.Instant
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "clock_time_get")
private external fun wasiRawClockTimeGet(clockId: Int, precision: Long, resultPtr: Int): Int

private const val CLOCKID_REALTIME = 0

@OptIn(UnsafeWasmMemoryApi::class)
private fun clockTimeGet(): Long = withScopedMemoryAllocator { allocator ->
    val rp0 = allocator.allocate(8)
    val ret = wasiRawClockTimeGet(
        clockId = CLOCKID_REALTIME,
        precision = 1,
        resultPtr = rp0.address.toInt()
    )
    if (ret == 0) {
        rp0.loadLong()
    } else {
        error("WASI call failed with $ret")
    }
}

internal actual fun currentTime(): Instant = clockTimeGet().let { time ->
    // Instant.MAX and Instant.MIN are never going to be exceeded using just the Long number of nanoseconds
    Instant(time.floorDiv(NANOS_PER_ONE.toLong()), time.mod(NANOS_PER_ONE.toLong()).toInt())
}

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZoneRules?> =
    "UTC" to null

internal actual val systemTzdb: TimeZoneDatabase = TzdbOnData()