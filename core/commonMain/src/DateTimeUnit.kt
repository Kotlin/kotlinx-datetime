/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.nanoseconds

// TODO: toString
sealed class DateTimeUnit {

    abstract operator fun times(scalar: Int): DateTimeUnit

    internal abstract val calendarUnit: CalendarUnit
    internal abstract val calendarScale: Long

    class TimeBased(val nanoseconds: Long) : DateTimeUnit() {
        internal override val calendarUnit: CalendarUnit
        internal override val calendarScale: Long

        init {
            require(nanoseconds > 0) { "Unit duration must be positive, but was $nanoseconds ns." }
            when {
                nanoseconds % 3600_000_000_000 == 0L -> {
                    calendarUnit = CalendarUnit.HOUR
                    calendarScale = nanoseconds / 3600_000_000_000
                }
                nanoseconds % 60_000_000_000 == 0L -> {
                    calendarUnit = CalendarUnit.MINUTE
                    calendarScale = nanoseconds / 60_000_000_000
                }
                nanoseconds % 1_000_000_000 == 0L -> {
                    calendarUnit = CalendarUnit.SECOND
                    calendarScale = nanoseconds / 1_000_000_000
                }
                nanoseconds % 1_000_000 == 0L -> {
                    calendarUnit = CalendarUnit.MILLISECOND
                    calendarScale = nanoseconds / 1_000_000
                }
                nanoseconds % 1_000 == 0L -> {
                    calendarUnit = CalendarUnit.MICROSECOND
                    calendarScale = nanoseconds / 1_000
                }
                else -> {
                    calendarUnit = CalendarUnit.NANOSECOND
                    calendarScale = nanoseconds
                }
            }
        }

        override fun times(scalar: Int): TimeBased = TimeBased(nanoseconds * scalar) // TODO: prevent overflow

        @ExperimentalTime
        val duration: Duration = nanoseconds.nanoseconds

        override fun equals(other: Any?): Boolean =
                this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()
    }

    sealed class DateBased : DateTimeUnit() {
        // TODO: investigate how to move subclasses to ChronoUnit scope
        class DayBased(val days: Int) : DateBased() {
            init {
                require(days > 0) { "Unit duration must be positive, but was $days days." }
            }

            override fun times(scalar: Int): DayBased = DayBased(days * scalar)

            internal override val calendarUnit: CalendarUnit get() = CalendarUnit.DAY
            internal override val calendarScale: Long get() = days.toLong()

            override fun equals(other: Any?): Boolean =
                    this === other || (other is DayBased && this.days == other.days)

            override fun hashCode(): Int = days xor 0x10000
        }
        class MonthBased(val months: Int) : DateBased() {
            init {
                require(months > 0) { "Unit duration must be positive, but was $months months." }
            }

            override fun times(scalar: Int): MonthBased = MonthBased(months * scalar)

            internal override val calendarUnit: CalendarUnit get() = CalendarUnit.MONTH
            internal override val calendarScale: Long get() = months.toLong()

            override fun equals(other: Any?): Boolean =
                    this === other || (other is MonthBased && this.months == other.months)

            override fun hashCode(): Int = months xor 0x20000
        }
    }


    companion object {
        val NANOSECOND = TimeBased(nanoseconds = 1)
        val MICROSECOND = NANOSECOND * 1000
        val MILLISECOND = MICROSECOND * 1000
        val SECOND = MILLISECOND * 1000
        val MINUTE = SECOND * 60
        val HOUR = MINUTE * 60
        val DAY = DateBased.DayBased(days = 1)
        val WEEK = DAY * 7
        val MONTH = DateBased.MonthBased(months = 1)
        val QUARTER = MONTH * 3
        val YEAR = MONTH * 12
        val CENTURY = YEAR * 100
    }
}


internal enum class CalendarUnit {
    YEAR,
    MONTH,
    DAY,
    HOUR,
    MINUTE,
    SECOND,
    MILLISECOND,
    MICROSECOND,
    NANOSECOND
}
