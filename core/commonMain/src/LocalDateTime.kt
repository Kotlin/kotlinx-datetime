/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime



public expect class LocalDateTime : Comparable<LocalDateTime> {
    companion object {
        public fun parse(isoString: String): LocalDateTime
    }

    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int)

    public val year: Int
    public val monthNumber: Int
    public val month: Month
    public val dayOfMonth: Int
    public val dayOfWeek: DayOfWeek
    public val dayOfYear: Int
    public val hour: Int
    public val minute: Int
    public val second: Int
    public val nanosecond: Int

    public val date: LocalDate

    public override operator fun compareTo(other: LocalDateTime): Int
}

public fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

public expect fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime
public expect fun LocalDateTime.toInstant(timeZone: TimeZone): Instant



expect fun LocalDateTime.plus(value: Long, unit: CalendarUnit): LocalDateTime
expect fun LocalDateTime.plus(value: Int, unit: CalendarUnit): LocalDateTime
expect operator fun LocalDateTime.plus(period: CalendarPeriod): LocalDateTime
expect operator fun LocalDateTime.minus(other: LocalDateTime): CalendarPeriod
