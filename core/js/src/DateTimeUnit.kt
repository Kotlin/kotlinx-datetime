/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.UnsupportedDateTimeUnitException
import kotlinx.datetime.internal.JSJoda.ChronoUnit as jsChronoUnit


/**
 * Convert this [DateTimeUnit] to the corresponding [jsChronoUnit].
 *
 * For [DateTimeUnit.DAY], this function will ignore the effect of Daylight Saving Time, and will always treat it as 24 hours.
 *
 * Note: this conversion is not always possible, and in such cases an exception is thrown.
 *
 * @return related [jsChronoUnit]
 */
public fun DateTimeUnit.toJsChronoUnit(): jsChronoUnit = when (this) {
    DateTimeUnit.NANOSECOND -> jsChronoUnit.NANOS
    DateTimeUnit.MICROSECOND -> jsChronoUnit.MICROS
    DateTimeUnit.MILLISECOND -> jsChronoUnit.MILLIS
    DateTimeUnit.SECOND -> jsChronoUnit.SECONDS
    DateTimeUnit.MINUTE -> jsChronoUnit.MINUTES
    DateTimeUnit.HOUR -> jsChronoUnit.HOURS
    DateTimeUnit.DAY -> jsChronoUnit.DAYS
    DateTimeUnit.WEEK -> jsChronoUnit.WEEKS
    DateTimeUnit.MONTH -> jsChronoUnit.MONTHS
    DateTimeUnit.YEAR -> jsChronoUnit.YEARS
    DateTimeUnit.CENTURY -> jsChronoUnit.CENTURIES
    is DateTimeUnit.DayBased ->
        when (this.days) {
            1 -> jsChronoUnit.DAYS
            7 -> jsChronoUnit.WEEKS
            else -> throw UnsupportedDateTimeUnitException("Unsupported day-based unit: $this")
        }
    is DateTimeUnit.MonthBased ->
        when (this.months) {
            1 -> jsChronoUnit.MONTHS
            12 -> jsChronoUnit.YEARS
            else -> throw UnsupportedDateTimeUnitException("Unsupported month-based unit: $this")
        }
    is DateTimeUnit.TimeBased ->
        when (this.unitName) {
            "NANOSECOND" -> jsChronoUnit.NANOS
            "MICROSECOND" -> jsChronoUnit.MICROS
            "MILLISECOND" -> jsChronoUnit.MILLIS
            "SECOND" -> jsChronoUnit.SECONDS
            "MINUTE" -> jsChronoUnit.MINUTES
            "HOUR" -> jsChronoUnit.HOURS
            else -> throw UnsupportedDateTimeUnitException("Unsupported time-based unit: $this")
        }
}