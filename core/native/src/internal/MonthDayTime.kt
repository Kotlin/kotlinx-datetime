/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.datetime.*

/**
 * A rule expressing how to create a date in a given year.
 *
 * Some examples of expressible dates:
 *  * the 16th March
 *  * the Sunday on or after the 16th March
 *  * the Sunday on or before the 16th March
 *  * the last Sunday in February
 *  * the 300th day of the year
 *  * the last day of February
 */
internal interface DateOfYear {
    /**
     * Converts this datetime to an [Instant] in the given [year],
     * using the knowledge of the offset that's in effect at the resulting datetime.
     */
    fun toLocalDate(year: Int): LocalDate
}

/**
 * The day of year, in the 0..365 range. During leap years, 29th February is counted as the 60th day of the year.
 * The number 366 is not supported, as outside the leap years, there are only 365 days in a year.
 */
internal class JulianDayOfYear(val zeroBasedDayOfYear: Int) : DateOfYear {
    init {
        require(zeroBasedDayOfYear in 0..365) {
            "Expected a value in 1..365 for the Julian day-of-year, but got $zeroBasedDayOfYear"
        }
    }
    override fun toLocalDate(year: Int): LocalDate =
        LocalDate(year, 1, 1).plusDays(zeroBasedDayOfYear)

    override fun toString(): String = "JulianDayOfYear($zeroBasedDayOfYear)"
}

/**
 * The day of year, in the 1..365 range. During leap years, 29th February is skipped.
 */
internal fun JulianDayOfYearSkippingLeapDate(dayOfYear: Int) : DateOfYear {
    require(dayOfYear in 1..365) {
        "Expected a value in 1..365 for the Julian day-of-year (skipping the leap date), but got $dayOfYear"
    }
    // In this form, the `dayOfYear` corresponds exactly to a specific month and day.
    // For example, `dayOfYear = 60` is always 1st March, even in leap years.
    // We take a non-leap year, as in that case, this is the same as JulianDayOfYear, so regular addition works.
    val date = LocalDate(2011, 1, 1).plusDays(dayOfYear - 1)
    return MonthDayOfYear(date.month, MonthDayOfYear.TransitionDay.ExactlyDayOfMonth(date.day))
}

internal class MonthDayOfYear(val month: Month, val day: TransitionDay) : DateOfYear {
    override fun toLocalDate(year: Int): LocalDate = day.resolve(year, month)

    /**
     * The day of month when the transition occurs.
     */
    sealed interface TransitionDay {
        /**
         * The first given [dayOfWeek] of the month that is not earlier than [atLeastDayOfMonth].
         */
        class First(val dayOfWeek: DayOfWeek, val atLeastDayOfMonth: Int = 1) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate =
                LocalDate(year, month, atLeastDayOfMonth).nextOrSame(dayOfWeek)

            override fun toString(): String = "the first $dayOfWeek" +
                (if (atLeastDayOfMonth > 1) " on or after $atLeastDayOfMonth" else "")
        }

        companion object {
            /**
             * The [n]th given [dayOfWeek] in the month.
             */
            fun Nth(dayOfWeek: DayOfWeek, n: Int): TransitionDay =
                First(dayOfWeek, (n-1) * 7 + 1)
        }

        /**
         * The last given [dayOfWeek] of the month that is not later than [atMostDayOfMonth].
         */
        class Last(val dayOfWeek: DayOfWeek, val atMostDayOfMonth: Int?) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate {
                val dayOfMonth = atMostDayOfMonth ?: month.number.monthLength(isLeapYear(year))
                return LocalDate(year, month, dayOfMonth).previousOrSame(dayOfWeek)
            }

            override fun toString(): String = "the last $dayOfWeek" +
                (atMostDayOfMonth?.let { " on or before $it" } ?: "")
        }

        /**
         * Exactly the given [dayOfMonth].
         */
        class ExactlyDayOfMonth(val dayOfMonth: Int) : TransitionDay {
            override fun resolve(year: Int, month: Month): LocalDate = LocalDate(year, month, dayOfMonth)

            override fun toString(): String = "$dayOfMonth"
        }

        fun resolve(year: Int, month: Month): LocalDate
    }

    override fun toString(): String = "$month, $day"
}

internal class MonthDayTime(
    /**
     * The date.
     */
    val date: DateOfYear,
    /**
     * The procedure to calculate the local time.
     */
    val time: TransitionLocaltime,
    /**
     * The definition of how the offset in which the local datetime is expressed.
     */
    val offset: OffsetResolver,
) {

    /**
     * Converts this [MonthDayTime] to an [Instant] in the given [year],
     * using the knowledge of the offset that's in effect at the resulting datetime.
     */
    fun toInstant(year: Int, effectiveOffset: UtcOffset): Instant {
        val localDateTime = time.resolve(date.toLocalDate(year))
        return when (this.offset) {
            is OffsetResolver.WallClockOffset -> localDateTime.toInstant(effectiveOffset)
            is OffsetResolver.FixedOffset -> localDateTime.toInstant(this.offset.offset)
        }
    }

    /**
     * Describes how the offset in which the local datetime is expressed is defined.
     */
    sealed interface OffsetResolver {
        /**
         * The offset is the one currently used by the wall clock.
         */
        object WallClockOffset : OffsetResolver {
            override fun toString(): String = "wall clock offset"
        }

        /**
         * The offset is fixed to a specific value.
         */
        class FixedOffset(val offset: UtcOffset) : OffsetResolver {
            override fun toString(): String = offset.toString()
        }
    }

    /**
     * The local time of day at which the transition occurs.
     */
    class TransitionLocaltime(val seconds: Int) {
        constructor(time: LocalTime) : this(time.toSecondOfDay())

        constructor(hour: Int, minute: Int, second: Int) : this(hour * 3600 + minute * 60 + second)

        fun resolve(date: LocalDate): LocalDateTime = date.atTime(LocalTime(0, 0)).plusSeconds(seconds)

        override fun toString(): String = if (seconds < 86400)
            LocalTime.ofSecondOfDay(seconds, 0).toString() else "$seconds seconds since the day start"
    }

    override fun toString(): String = "$date, $time, $offset"
}
