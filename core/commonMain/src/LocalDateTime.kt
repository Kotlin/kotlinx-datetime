/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime



public expect class LocalDateTime : Comparable<LocalDateTime> {
    companion object {
        /**
         * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [LocalDateTime] are
         * exceeded.
         */
        public fun parse(isoString: String): LocalDateTime

        internal val MIN: LocalDateTime
        internal val MAX: LocalDateTime
    }

    /**
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for [month] and
     * [year].
     */
    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0)

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

/**
 * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [LocalDateTime] are exceeded.
 */
public fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

