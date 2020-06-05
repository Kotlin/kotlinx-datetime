/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration


@OptIn(kotlin.time.ExperimentalTime::class)
public expect class Instant : Comparable<Instant> {

    public val epochSeconds: Long
    public val nanosecondsOfSecond: Int

    public fun toEpochMilliseconds(): Long

    public operator fun plus(duration: Duration): Instant
    public operator fun minus(duration: Duration): Instant

    // questionable
    public operator fun minus(other: Instant): Duration

    public override operator fun compareTo(other: Instant): Int

    companion object {
        fun now(): Instant
        fun fromEpochMilliseconds(epochMilliseconds: Long): Instant
        fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long = 0): Instant
        fun parse(isoString: String): Instant
    }
}

public fun String.toInstant(): Instant = Instant.parse(this)

public expect fun Instant.plus(period: CalendarPeriod, zone: TimeZone): Instant
public expect fun Instant.plus(value: Int, unit: CalendarUnit, zone: TimeZone): Instant
public expect fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant

public expect fun Instant.periodUntil(other: Instant, zone: TimeZone): CalendarPeriod
public expect fun Instant.until(other: Instant, unit: CalendarUnit, zone: TimeZone): Long
public expect fun Instant.daysUntil(other: Instant, zone: TimeZone): Int
public expect fun Instant.monthsUntil(other: Instant, zone: TimeZone): Int
public expect fun Instant.yearsUntil(other: Instant, zone: TimeZone): Int
