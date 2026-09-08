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
open class TimeZoneOperations {
    val timeZone = TimeZoneContext.System.get("Europe/Berlin")
    val dateIn1970 = LocalDate(1970, 1, 1)
    val dateTimeIn1970 = dateIn1970.atTime(12, 30)
    val instantIn1970 = kotlin.time.Instant.fromEpochMilliseconds(0)
    val dateToday = Clock.System.todayIn(timeZone)
    val dateTimeToday = dateToday.atTime(12, 30)
    val instantToday = Clock.System.now()

    @Benchmark
    fun atStartOfDayRecurringRules() = dateToday.atStartOfDayIn(timeZone)

    @Benchmark
    fun atStartOfDayHistoricRules() = dateIn1970.atStartOfDayIn(timeZone)

    @Benchmark
    fun localDateTimeToInstantRecurringRules() = dateTimeToday.toInstant(timeZone, TransitionHandler.USE_OFFSET_BEFORE)

    @Benchmark
    fun localDateTimeToInstantHistoricRules() = dateTimeIn1970.toInstant(timeZone, TransitionHandler.USE_OFFSET_BEFORE)

    @Benchmark
    fun instantToLocalDateTimeRecurringRules() = instantToday.toLocalDateTime(timeZone)

    @Benchmark
    fun instantToLocalDateTimeHistoricRules() = instantIn1970.toLocalDateTime(timeZone)

    @Benchmark
    fun offsetInRecurringRules() = instantToday.offsetIn(timeZone)

    @Benchmark
    fun offsetInHistoricRules() = instantIn1970.offsetIn(timeZone)

    @Benchmark
    fun offsetInfoForRecurringRules() = timeZone.offsetInfoFor(dateTimeToday)

    @Benchmark
    fun offsetInfoForHistoricRules() = timeZone.offsetInfoFor(dateTimeIn1970)
}