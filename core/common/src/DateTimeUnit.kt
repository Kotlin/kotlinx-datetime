/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A unit for measuring time.
 *
 * See the predefined constants for time units, like [DateTimeUnit.NANOSECOND], [DateTimeUnit.DAY],
 * [DateTimeUnit.MONTH], or others.
 *
 * Two ways are provided to create custom [DateTimeUnit] instances:
 * - By multiplying an existing unit on the right by a scalar: for example, `DateTimeUnit.NANOSECOND * 10`.
 * - By constructing an instance manually with [TimeBased], [DayBased], or [MonthBased]: for example,
 *   `TimeBased(nanoseconds = 10)`.
 *
 * Note that a day is not considered equal to 24 hours. See [DateTimeUnit.DAY] for a discussion.
 */
@Serializable(with = DateTimeUnitSerializer::class)
public sealed class DateTimeUnit {

    public abstract operator fun times(scalar: Int): DateTimeUnit

    /**
     * The date-time units that are independent of the time zone.
     *
     * Any such unit can be represented as some number of nanoseconds.
     */
    @Serializable(with = TimeBasedDateTimeUnitSerializer::class)
    public class TimeBased(
        /**
         * The length of this unit in nanoseconds.
         */
        public val nanoseconds: Long
    ) : DateTimeUnit() {
        private val unitName: String
        private val unitScale: Long

        init {
            require(nanoseconds > 0) { "Unit duration must be positive, but was $nanoseconds ns." }
            // find a concise string representation for the unit with this duration
            when {
                nanoseconds % 3600_000_000_000 == 0L -> {
                    unitName = "HOUR"
                    unitScale = nanoseconds / 3600_000_000_000
                }
                nanoseconds % 60_000_000_000 == 0L -> {
                    unitName = "MINUTE"
                    unitScale = nanoseconds / 60_000_000_000
                }
                nanoseconds % 1_000_000_000 == 0L -> {
                    unitName = "SECOND"
                    unitScale = nanoseconds / 1_000_000_000
                }
                nanoseconds % 1_000_000 == 0L -> {
                    unitName = "MILLISECOND"
                    unitScale = nanoseconds / 1_000_000
                }
                nanoseconds % 1_000 == 0L -> {
                    unitName = "MICROSECOND"
                    unitScale = nanoseconds / 1_000
                }
                else -> {
                    unitName = "NANOSECOND"
                    unitScale = nanoseconds
                }
            }
        }

        override fun times(scalar: Int): TimeBased = TimeBased(safeMultiply(nanoseconds, scalar.toLong()))

        /**
         * The length of this unit as a [Duration].
         */
        public val duration: Duration
            get() = nanoseconds.nanoseconds

        override fun equals(other: Any?): Boolean =
            this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()

        override fun toString(): String = formatToString(unitScale, unitName)
    }

    /**
     * The date-time unit equal to some number of days or months.
     *
     * Unless used for operations on dates, measuring with such units requires knowledge of the time zone, as it is
     * otherwise unknown when one date ends and another one begins.
     */
    @Serializable(with = DateBasedDateTimeUnitSerializer::class)
    public sealed class DateBased : DateTimeUnit() {
        @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
        @Deprecated("Use DateTimeUnit.DayBased", ReplaceWith("DateTimeUnit.DayBased", "kotlinx.datetime.DateTimeUnit"))
        public typealias DayBased = DateTimeUnit.DayBased
        @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
        @Deprecated("Use DateTimeUnit.MonthBased", ReplaceWith("DateTimeUnit.MonthBased", "kotlinx.datetime.DateTimeUnit"))
        public typealias MonthBased = DateTimeUnit.MonthBased
    }

    /**
     * The date-time unit equal to some number of days.
     *
     * A day is not considered equal to 24 hours. See [DateTimeUnit.DAY] for a discussion.
     */
    @Serializable(with = DayBasedDateTimeUnitSerializer::class)
    public class DayBased(
        /**
         * The length of this unit in days.
         */
        public val days: Int
    ) : DateBased() {
        init {
            require(days > 0) { "Unit duration must be positive, but was $days days." }
        }

        override fun times(scalar: Int): DateTimeUnit.DayBased = DateTimeUnit.DayBased(safeMultiply(days, scalar))

        override fun equals(other: Any?): Boolean =
            this === other || (other is DateTimeUnit.DayBased && this.days == other.days)

        override fun hashCode(): Int = days xor 0x10000

        override fun toString(): String = if (days % 7 == 0)
            formatToString(days / 7, "WEEK")
        else
            formatToString(days, "DAY")
    }

    /**
     * The date-time unit equal to some number of months.
     */
    @Serializable(with = MonthBasedDateTimeUnitSerializer::class)
    public class MonthBased(
        /**
         * The length of this unit in months.
         */
        public val months: Int
    ) : DateBased() {
        init {
            require(months > 0) { "Unit duration must be positive, but was $months months." }
        }

        override fun times(scalar: Int): DateTimeUnit.MonthBased = DateTimeUnit.MonthBased(safeMultiply(months, scalar))

        override fun equals(other: Any?): Boolean =
            this === other || (other is DateTimeUnit.MonthBased && this.months == other.months)

        override fun hashCode(): Int = months xor 0x20000

        override fun toString(): String = when {
            months % 12_00 == 0 -> formatToString(months / 12_00, "CENTURY")
            months % 12 == 0 -> formatToString(months / 12, "YEAR")
            months % 3 == 0 -> formatToString(months / 3, "QUARTER")
            else -> formatToString(months, "MONTH")
        }
    }

    protected fun formatToString(value: Int, unit: String): String = if (value == 1) unit else "$value-$unit"
    protected fun formatToString(value: Long, unit: String): String = if (value == 1L) unit else "$value-$unit"

    public companion object {
        /**
         * A nanosecond, which is `1/1_000_000_000` of a second.
         */
        public val NANOSECOND: TimeBased = TimeBased(nanoseconds = 1)

        /**
         * A microsecond, which is `1/1_000_000` of a second, or `1_000` nanoseconds.
         */
        public val MICROSECOND: TimeBased = NANOSECOND * 1000

        /**
         * A millisecond, which is `1/1_000` of a second, or `1_000_000` nanoseconds.
         */
        public val MILLISECOND: TimeBased = MICROSECOND * 1000

        /**
         * A second.
         */
        public val SECOND: TimeBased = MILLISECOND * 1000

        /**
         * A minute, which is 60 seconds.
         */
        public val MINUTE: TimeBased = SECOND * 60

        /**
         * An hour, which is 60 minutes, or 3600 seconds.
         */
        public val HOUR: TimeBased = MINUTE * 60

        /**
         * A day.
         *
         * We follow ISO-8601 in that we do not consider a day to be the same as 24 hours.
         * The reason lies in time zone transitions, because of which some days can be 23 or 25 hours.
         * For example, we say that exactly a whole day has passed between `2019-10-27T02:59` and `2019-10-28T02:59`
         * in Berlin, despite the fact that the clocks were turned back one hour, so there are, in fact, 25 hours
         * between the two date-times.
         *
         * It is for this reason that most operations with [DateBased] units require a [TimeZone], whereas those with
         * [TimeBased] ones never do.
         */
        public val DAY: DayBased = DayBased(days = 1)

        /**
         * 7 days.
         *
         * It can not represented as some number of hours. Please see [DAY] for a discussion.
         */
        public val WEEK: DayBased = DAY * 7

        /**
         * A month.
         */
        public val MONTH: MonthBased = MonthBased(months = 1)

        /**
         * Three months.
         */
        public val QUARTER: MonthBased = MONTH * 3

        /**
         * 12 months.
         */
        public val YEAR: MonthBased = MONTH * 12

        /**
         * 100 years, or 1200 months.
         */
        public val CENTURY: MonthBased = YEAR * 100
    }
}
