/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.*

@Serializable(with = DateTimeUnitSerializer::class)
public sealed class DateTimeUnit {

    public abstract operator fun times(scalar: Int): DateTimeUnit

    @Serializable(with = TimeBasedDateTimeUnitSerializer::class)
    public class TimeBased(public val nanoseconds: Long) : DateTimeUnit() {
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

        @ExperimentalTime
        public val duration: Duration
            get() = Duration.nanoseconds(nanoseconds)

        override fun equals(other: Any?): Boolean =
                this === other || (other is TimeBased && this.nanoseconds == other.nanoseconds)

        override fun hashCode(): Int = nanoseconds.toInt() xor (nanoseconds shr Int.SIZE_BITS).toInt()

        override fun toString(): String = formatToString(unitScale, unitName)
    }

    @Serializable(with = DateBasedDateTimeUnitSerializer::class)
    public sealed class DateBased : DateTimeUnit() {
        // TODO: investigate how to move subclasses up to DateTimeUnit scope
        @Serializable(with = DayBasedDateTimeUnitSerializer::class)
        public class DayBased(public val days: Int) : DateBased() {
            init {
                require(days > 0) { "Unit duration must be positive, but was $days days." }
            }

            override fun times(scalar: Int): DayBased = DayBased(safeMultiply(days, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is DayBased && this.days == other.days)

            override fun hashCode(): Int = days xor 0x10000

            override fun toString(): String = if (days % 7 == 0)
                formatToString(days / 7, "WEEK")
            else
                formatToString(days, "DAY")
        }
        @Serializable(with = MonthBasedDateTimeUnitSerializer::class)
        public class MonthBased(public val months: Int) : DateBased() {
            init {
                require(months > 0) { "Unit duration must be positive, but was $months months." }
            }

            override fun times(scalar: Int): MonthBased = MonthBased(safeMultiply(months, scalar))

            override fun equals(other: Any?): Boolean =
                    this === other || (other is MonthBased && this.months == other.months)

            override fun hashCode(): Int = months xor 0x20000

            override fun toString(): String = when {
                months % 12_00 == 0 -> formatToString(months / 12_00, "CENTURY")
                months % 12 == 0 -> formatToString(months / 12, "YEAR")
                months % 3 == 0 -> formatToString(months / 3, "QUARTER")
                else -> formatToString(months, "MONTH")
            }
        }
    }

    protected fun formatToString(value: Int, unit: String): String = if (value == 1) unit else "$value-$unit"
    protected fun formatToString(value: Long, unit: String): String = if (value == 1L) unit else "$value-$unit"

    public companion object {
        public val NANOSECOND: TimeBased = TimeBased(nanoseconds = 1)
        public val MICROSECOND: TimeBased = NANOSECOND * 1000
        public val MILLISECOND: TimeBased = MICROSECOND * 1000
        public val SECOND: TimeBased = MILLISECOND * 1000
        public val MINUTE: TimeBased = SECOND * 60
        public val HOUR: TimeBased = MINUTE * 60
        public val DAY: DateBased.DayBased = DateBased.DayBased(days = 1)
        public val WEEK: DateBased.DayBased = DAY * 7
        public val MONTH: DateBased.MonthBased = DateBased.MonthBased(months = 1)
        public val QUARTER: DateBased.MonthBased = MONTH * 3
        public val YEAR: DateBased.MonthBased = MONTH * 12
        public val CENTURY: DateBased.MonthBased = YEAR * 100
    }
}
