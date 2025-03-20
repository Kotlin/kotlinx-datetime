/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.YearMonthIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable(with = YearMonthIso8601Serializer::class)
public actual class YearMonth
public actual constructor(year: Int, month: Int) : Comparable<YearMonth>, java.io.Serializable {
    public actual val year: Int = year
    internal actual val monthNumber: Int = month

    init {
        require(month in 1..12) { "Month must be in 1..12, but was $month" }
        require(year in LocalDate.MIN.year..LocalDate.MAX.year) {
            "Year $year is out of range: ${LocalDate.MIN.year}..${LocalDate.MAX.year}"
        }
    }

    public actual val month: Month get() = Month(monthNumber)
    public actual val firstDay: LocalDate get() = onDay(1)
    public actual val lastDay: LocalDate get() = onDay(numberOfDays)
    public actual val numberOfDays: Int get() = monthNumber.monthLength(isLeapYear(year))

    // val days: LocalDateRange get() = firstDay..lastDay // no ranges yet

    public actual constructor(year: Int, month: Month): this(year, month.number)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<YearMonth>): YearMonth =
            format.parse(input)

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithYearMonth.() -> Unit): DateTimeFormat<YearMonth> =
            YearMonthFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<YearMonth> get() = ISO_YEAR_MONTH
    }

    actual override fun compareTo(other: YearMonth): Int =
        compareValuesBy(this, other, YearMonth::year, YearMonth::month)

    actual override fun toString(): String = Formats.ISO.format(this)

    override fun equals(other: Any?): Boolean = other is YearMonth && year == other.year && month == other.month

    override fun hashCode(): Int = year * 31 + month.hashCode()

    private fun writeReplace(): Any = Ser(Ser.YEAR_MONTH_TAG, this)
}

internal fun YearMonth.toEpochMonths(): Long = (year - 1970L) * 12 + monthNumber - 1

internal fun YearMonth.Companion.fromEpochMonths(months: Long): YearMonth {
    val year = months.floorDiv(12) + 1970
    val month = months.mod(12) + 1
    return YearMonth(year.toInt(), month)
}
