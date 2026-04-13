/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.YearMonthSerializer
import kotlinx.serialization.Serializable

@Serializable(with = YearMonthSerializer::class)
public actual class YearMonth
private constructor(public actual val year: Int, month: Int, unit: Unit) : Comparable<YearMonth> {
    internal actual val monthNumber: Int = month

    private fun validateYear() {
        require(year in YEAR_MIN..YEAR_MAX) {
            "Year $year is out of range: $YEAR_MIN..$YEAR_MAX"
        }
    }

    public actual val month: Month get() = Month(monthNumber)

    public actual val firstDay: LocalDate get() = onDay(1)

    public actual val lastDay: LocalDate get() = onDay(numberOfDays)

    public actual val numberOfDays: Int get() = monthNumber.monthLength(isLeapYear(year))

    public actual val days: LocalDateRange get() = firstDay..lastDay // no ranges yet

    public actual constructor(year: Int, month: Int): this(year, month, Unit) {
        require(monthNumber in 1..12) { "Month must be in 1..12, but was $month" }
        validateYear()
    }

    public actual constructor(year: Int, month: Month): this(year, month.number, Unit) {
        validateYear()
    }

    public actual companion object {
        public actual fun orNull(year: Int, month: Int): YearMonth? =
            if (year !in YEAR_MIN..YEAR_MAX || month !in 1..12) null else YearMonth(year, month, Unit)

        public actual fun orNull(year: Int, month: Month): YearMonth? =
            if (year !in YEAR_MIN..YEAR_MAX) null else YearMonth(year, month.number, Unit)

        public actual fun parse(input: CharSequence, format: DateTimeFormat<YearMonth>): YearMonth =
            format.parse(input)

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithYearMonth.() -> Unit): DateTimeFormat<YearMonth> =
            YearMonthFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<YearMonth> get() = ISO_YEAR_MONTH
    }

    public actual operator fun rangeTo(that: YearMonth): YearMonthRange = YearMonthRange.fromRangeTo(this, that)

    public actual operator fun rangeUntil(that: YearMonth): YearMonthRange = YearMonthRange.fromRangeUntil(this, that)

    actual override fun compareTo(other: YearMonth): Int =
        compareValuesBy(this, other, YearMonth::year, YearMonth::month)

    actual override fun toString(): String = Formats.ISO.format(this)

    override fun equals(other: Any?): Boolean = other is YearMonth && year == other.year && month == other.month

    override fun hashCode(): Int = year * 31 + month.hashCode()
}
