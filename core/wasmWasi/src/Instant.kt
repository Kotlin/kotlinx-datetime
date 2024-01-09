/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.NANOS_PER_ONE
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.Pointer
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
        (Pointer(rp0.address.toInt().toUInt())).loadLong()
    } else {
        error("WASI call failed with $ret")
    }
}

internal actual fun currentTime(): Instant = clockTimeGet().let { time ->
    Instant(time / NANOS_PER_ONE, (time % NANOS_PER_ONE).toInt())
}