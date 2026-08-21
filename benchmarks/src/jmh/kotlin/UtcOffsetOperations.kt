/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class UtcOffsetOperations {
    val dateToday = Clock.System.todayIn(TimeZone.UTC)
    val dateTimeToday = dateToday.atTime(12, 30)
    val instantToday = Clock.System.now()
    val offset = UtcOffset(hours = 2)

    @Benchmark
    fun localDateTimeToInstant() = dateTimeToday.toInstant(offset)

    @Suppress("INVISIBLE_REFERENCE")
    @Benchmark
    fun instantToLocalDateTime() = instantToday.toLocalDateTime(offset)
}