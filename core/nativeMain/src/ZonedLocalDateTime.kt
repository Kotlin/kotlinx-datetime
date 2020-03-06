/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

internal class ZonedLocalDateTime(val dateTime: LocalDateTime, val zone: TimeZone, val offset: ZoneOffset) {
    internal fun plusYears(years: Long): ZonedLocalDateTime = resolve(dateTime.plusYears(years))
    internal fun plusMonths(months: Long): ZonedLocalDateTime = resolve(dateTime.plusMonths(months))
    internal fun plusDays(days: Long): ZonedLocalDateTime = resolve(dateTime.plusDays(days))

    fun resolve(dateTime: LocalDateTime): ZonedLocalDateTime =
        ZonedLocalDateTime(dateTime, zone, with(zone) { dateTime.presumedOffset(offset) })
}

internal fun ZonedLocalDateTime.until(other: ZonedLocalDateTime, unit: CalendarUnit): Long =
    when (unit) {
        CalendarUnit.YEAR, CalendarUnit.MONTH, CalendarUnit.WEEK, CalendarUnit.DAY -> {
            dateTime.until(other.dateTime, unit)
        }
        CalendarUnit.HOUR, CalendarUnit.MINUTE, CalendarUnit.SECOND, CalendarUnit.NANOSECOND -> {
            val offsetDiff = offset.totalSeconds - other.offset.totalSeconds
            dateTime.until(other.dateTime.plusSeconds(offsetDiff.toLong()), unit)
        }
    }
