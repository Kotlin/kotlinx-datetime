/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime
import java.time.temporal.ChronoUnit as jtChronoUnit

/**
 * Convert this [DateTimeUnit] to the corresponding [jtChronoUnit].
 *
 * For [DateTimeUnit.DAY], this function will ignore the effect of Daylight Saving Time, and will always treat it as 24 hours.
 *
 * Note: this conversion is not always possible, and in such cases an exception is thrown.
 *
 * @return
 */
@Throws(UnsupportedDateTimeUnitException::class)
public fun DateTimeUnit.toJavaChronoUnit(): jtChronoUnit = when (this) {
    DateTimeUnit.NANOSECOND -> jtChronoUnit.NANOS
    DateTimeUnit.MICROSECOND -> jtChronoUnit.MICROS
    DateTimeUnit.MILLISECOND -> jtChronoUnit.MILLIS
    DateTimeUnit.SECOND -> jtChronoUnit.SECONDS
    DateTimeUnit.MINUTE -> jtChronoUnit.MINUTES
    DateTimeUnit.HOUR -> jtChronoUnit.HOURS
    DateTimeUnit.DAY -> jtChronoUnit.DAYS
    DateTimeUnit.WEEK -> jtChronoUnit.WEEKS
    DateTimeUnit.MONTH -> jtChronoUnit.MONTHS
    DateTimeUnit.YEAR -> jtChronoUnit.YEARS
    DateTimeUnit.CENTURY -> jtChronoUnit.CENTURIES
    is DateTimeUnit.DayBased ->
        when (this.days) {
            1 -> jtChronoUnit.DAYS
            7 -> jtChronoUnit.WEEKS
            else -> throw UnsupportedDateTimeUnitException("Unsupported day-based unit: $this")
        }
    is DateTimeUnit.MonthBased ->
        when (this.months) {
            1 -> jtChronoUnit.MONTHS
            12 -> jtChronoUnit.YEARS
            else -> throw UnsupportedDateTimeUnitException("Unsupported month-based unit: $this")
        }
    is DateTimeUnit.TimeBased ->
        when (this.unitName) {
            "NANOSECOND" -> jtChronoUnit.NANOS
            "MICROSECOND" -> jtChronoUnit.MICROS
            "MILLISECOND" -> jtChronoUnit.MILLIS
            "SECOND" -> jtChronoUnit.SECONDS
            "MINUTE" -> jtChronoUnit.MINUTES
            "HOUR" -> jtChronoUnit.HOURS
            else -> throw UnsupportedDateTimeUnitException("Unsupported time-based unit: $this")
        }
}