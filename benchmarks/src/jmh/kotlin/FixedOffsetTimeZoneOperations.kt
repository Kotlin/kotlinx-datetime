/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import org.openjdk.jmh.annotations.*
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.toJavaInstant

enum class FixedOffsetTimeZoneParam(val timeZone: FixedOffsetTimeZone, val zoneId: ZoneId) {
    UTC(TimeZone.UTC, ZoneId.of("UTC")),
    UTC_PLUS_1_10_15(TimeZoneContext.System.get("UTC+01:10:15") as FixedOffsetTimeZone, ZoneId.of("UTC+01:10:15")),
    GMT(TimeZoneContext.System.get("GMT") as FixedOffsetTimeZone, ZoneId.of("GMT")),
    GMT_PLUS_1_10_15(TimeZoneContext.System.get("GMT+01:10:15") as FixedOffsetTimeZone, ZoneId.of("GMT+01:10:15")),
}

enum class UtcOffsetParam(val offset: UtcOffset) {
    UTC(UtcOffset.ZERO),
    UTC_PLUS_1_30_15(UtcOffset(1, 10, 15)),
}

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FixedOffsetTimeZoneConstruction {
    @Param lateinit var timeZone: UtcOffsetParam

    @Benchmark
    fun construct() = FixedOffsetTimeZone(timeZone.offset)
}

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FixedOffsetTimeZoneUnaryOperations {
    @Param lateinit var timeZone: FixedOffsetTimeZoneParam

    @Benchmark
    fun id() = timeZone.timeZone.id

    @Benchmark
    fun convertToKotlin() = timeZone.zoneId.toKotlinTimeZone()

    @Benchmark
    fun convertToJava() = timeZone.timeZone.toJavaZoneId()

    @Benchmark
    fun hashCodeImpl() = timeZone.timeZone.hashCode()

    @Benchmark
    fun toStringImpl() = timeZone.timeZone.toString()
}

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FixedOffsetTimeZoneBinaryOperations {
    @Param lateinit var timeZone: FixedOffsetTimeZoneParam
    @Param lateinit var otherTimeZone: FixedOffsetTimeZoneParam

    @Benchmark
    fun equalsImpl() = timeZone.timeZone == otherTimeZone.timeZone
}

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class FixedOffsetTimeZoneQueryOperations {
    @Param lateinit var timeZone: FixedOffsetTimeZoneParam
    val dateToday = Clock.System.todayIn(TimeZone.UTC)
    val javaDateToday = dateToday.toJavaLocalDate()
    val dateTimeToday = dateToday.atTime(12, 30)
    val javaDateTimeToday = dateTimeToday.toJavaLocalDateTime()
    val instantToday = Clock.System.now()
    val javaInstantToday = instantToday.toJavaInstant()

    @Benchmark
    fun atStartOfDayKotlin() = dateToday.atStartOfDayIn(timeZone.timeZone)

    @Benchmark
    fun atStartOfDayJava() = javaDateToday.atStartOfDay(timeZone.zoneId).toInstant()

    @Benchmark
    fun localDateTimeToInstantKotlin() = dateTimeToday.toInstant(timeZone.timeZone, TransitionHandler.USE_OFFSET_BEFORE)

    @Benchmark
    fun localDateTimeToInstantJava() = javaDateTimeToday.atZone(timeZone.zoneId).toInstant()

    @Benchmark
    fun instantToLocalDateTimeKotlin() = instantToday.toLocalDateTime(timeZone.timeZone)

    @Benchmark
    fun instantToLocalDateTimeJava() = javaInstantToday.atZone(timeZone.zoneId).toLocalDateTime()

    @Benchmark
    fun offsetInKotlin() = instantToday.offsetIn(timeZone.timeZone)

    @Benchmark
    fun offsetInJava() = timeZone.zoneId.rules.getOffset(javaInstantToday)

    @Benchmark
    fun offsetInfoForKotlin() = timeZone.timeZone.offsetInfoFor(dateTimeToday)

    @Benchmark
    // similar enough
    fun offsetInfoForJava() = timeZone.zoneId.rules.getValidOffsets(javaDateTimeToday)
}
