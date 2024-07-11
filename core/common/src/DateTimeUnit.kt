/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A unit for measuring time; for example, a second, 20 seconds, a day, a month, or a quarter.
 *
 * This class is used to express arithmetic operations like addition and subtraction on datetime values:
 * for example, adding 10 days to a datetime value, subtracting 5 hours from a datetime value, or finding the
 * number of 30-second intervals between two datetime values.
 *
 * ### Interaction with other entities
 *
 * Any [DateTimeUnit] can be used with [Instant.plus] or [Instant.minus] to
 * find an instant that is some number of units away from the given instant.
 * Also, [Instant.until] can be used to find the number of the given units between two instants.
 *
 * [DateTimeUnit.TimeBased] can be used in the [Instant] operations without specifying the time zone because
 * [DateTimeUnit.TimeBased] is defined in terms of the passage of real-time and is independent of the time zone.
 * Note that a calendar day is not considered identical to 24 hours, so using it does require specifying the time zone.
 * See [DateTimeUnit.DayBased] for an explanation.
 *
 * [DateTimeUnit.DateBased] units can be used in the [LocalDate] operations: [LocalDate.plus], [LocalDate.minus], and
 * [LocalDate.until].
 *
 * Arithmetic operations on [LocalDateTime] are not provided.
 * Please see the [LocalDateTime] documentation for details.
 *
 * [DateTimePeriod] is a combination of [DateTimeUnit] values of every kind, used to express periods like
 * "two days and three hours".
 * [DatePeriod] is specifically a combination of [DateTimeUnit.DateBased] values.
 * [DateTimePeriod] is more flexible than [DateTimeUnit] because it can express a combination of values with different
 * kinds of units. However, in exchange, the duration of time between two [Instant] or [LocalDate] values can be
 * measured in terms of some [DateTimeUnit], but not [DateTimePeriod] or [DatePeriod].
 *
 * ### Construction, serialization, and deserialization
 *
 * See the predefined constants for time units, like [DateTimeUnit.NANOSECOND], [DateTimeUnit.DAY],
 * [DateTimeUnit.MONTH], and others.
 *
 * Two ways are provided to create custom [DateTimeUnit] instances:
 * - By multiplying an existing unit on the right by an integer scalar, for example, `DateTimeUnit.MICROSECOND * 10`.
 * - By constructing an instance manually with [TimeBased], [DayBased], or [MonthBased], for example,
 *   `DateTimeUnit.TimeBased(nanoseconds = 10_000)`.
 *
 * Also, [DateTimeUnit] can be serialized and deserialized using `kotlinx.serialization`:
 * [DateTimeUnitSerializer], [DateBasedDateTimeUnitSerializer], [DayBasedDateTimeUnitSerializer],
 * [MonthBasedDateTimeUnitSerializer], and [TimeBasedDateTimeUnitSerializer] are provided, with varying levels of
 * specificity of the type they handle.
 *
 * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.construction
 */
@Serializable(with = DateTimeUnitSerializer::class)
public sealed class DateTimeUnit {

    /**
     * Produces a datetime unit that is a multiple of this unit times the specified integer [scalar] value.
     *
     * @throws ArithmeticException if the result overflows.
     * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.multiplication
     */
    public abstract operator fun times(scalar: Int): DateTimeUnit

    /**
     * A [datetime unit][DateTimeUnit] that has the precise time duration.
     *
     * Such units are independent of the time zone.
     * Any such unit can be represented as some fixed number of nanoseconds.
     *
     * @see DateTimeUnit for a description of datetime units in general.
     * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.timeBasedUnit
     */
    @Serializable(with = TimeBasedDateTimeUnitSerializer::class)
    public class TimeBased(
        /**
         * The length of this unit in nanoseconds.
         *
         * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.timeBasedUnit
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
         *
         * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.timeBasedUnit
         */
        public val duration: Duration
            get() = nanoseconds.nanoseconds

        override fun equals(other: Any?): Boolean =
            this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()

        override fun toString(): String = formatToString(unitScale, unitName)
    }

    /**
     * A [datetime unit][DateTimeUnit] equal to some number of days or months.
     *
     * Operations involving `DateBased` units are performed on dates. The same operations on [Instants][Instant]
     * require a [TimeZone] to find the corresponding [LocalDateTimes][LocalDateTime] first to perform
     * the operation with the date component of these `LocalDateTime` values.
     *
     * @see DateTimeUnit for a description of datetime units in general.
     * @see DateTimeUnit.DayBased for specifically day-based units.
     * @see DateTimeUnit.MonthBased for specifically month-based units.
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
     * A [datetime unit][DateTimeUnit] equal to some number of calendar days.
     *
     * A calendar day is not considered identical to 24 hours.
     * Thus, a `DayBased` unit cannot be expressed as a multiple of some [TimeBased] unit.
     *
     * The reason lies in time zone transitions, because of which some days can be 23 or 25 hours.
     * For example, we say that exactly a whole day has passed between `2019-10-27T02:59` and `2019-10-28T02:59`
     * in Berlin, even though the clocks were turned back one hour, so there are, in fact, 25 hours
     * between the two datetimes.
     *
     * @see DateTimeUnit for a description of datetime units in general.
     * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.dayBasedUnit
     */
    @Serializable(with = DayBasedDateTimeUnitSerializer::class)
    public class DayBased(
        /**
         * The length of this unit in days.
         *
         * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.dayBasedUnit
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
     * A [datetime unit][DateTimeUnit] equal to some number of months.
     *
     * Since different months have a different number of days, a `MonthBased` unit cannot be expressed
     * as a multiple of some [DayBased]-unit.
     *
     * @see DateTimeUnit for a description of datetime units in general.
     * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.monthBasedUnit
     */
    @Serializable(with = MonthBasedDateTimeUnitSerializer::class)
    public class MonthBased(
        /**
         * The length of this unit in months.
         *
         * @sample kotlinx.datetime.test.samples.DateTimeUnitSamples.monthBasedUnit
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
         * A calendar day.
         *
         * Note that a calendar day is not the same as 24 hours, see [DateTimeUnit.DayBased] for details.
         */
        public val DAY: DayBased = DayBased(days = 1)

        /**
         * A week, which is 7 [calendar days][DAY].
         */
        public val WEEK: DayBased = DAY * 7

        /**
         * A month.
         *
         * Note that a month doesn't have a constant number of calendar days in it.
         */
        public val MONTH: MonthBased = MonthBased(months = 1)

        /**
         * A quarter, which is three [months][MONTH].
         */
        public val QUARTER: MonthBased = MONTH * 3

        /**
         * A year, which is 12 [months][MONTH].
         */
        public val YEAR: MonthBased = MONTH * 12

        /**
         * A century, which is 100 [years][YEAR], or 1200 [months][MONTH].
         */
        public val CENTURY: MonthBased = YEAR * 100
    }
}
