/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal class ZonedDateTime(val dateTime: LocalDateTime, private val zone: TimeZone, val offset: ZoneOffset) {
    internal fun plusYears(years: Long): ZonedDateTime = resolve(dateTime.plusYears(years))
    internal fun plusMonths(months: Long): ZonedDateTime = resolve(dateTime.plusMonths(months))
    internal fun plusDays(days: Long): ZonedDateTime = resolve(dateTime.plusDays(days))

    private fun resolve(dateTime: LocalDateTime): ZonedDateTime =
        ZonedDateTime(dateTime, zone, with(zone) { dateTime.presumedOffset(offset) })
}

internal fun ZonedDateTime.until(other: ZonedDateTime, unit: CalendarUnit): Long =
    when (unit) {
        CalendarUnit.YEAR, CalendarUnit.MONTH, CalendarUnit.WEEK, CalendarUnit.DAY -> {
            dateTime.until(other.dateTime, unit)
        }
        CalendarUnit.HOUR, CalendarUnit.MINUTE, CalendarUnit.SECOND, CalendarUnit.NANOSECOND -> {
            val offsetDiff = offset.totalSeconds - other.offset.totalSeconds
            dateTime.until(other.dateTime.plusSeconds(offsetDiff.toLong()), unit)
        }
    }
