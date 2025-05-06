/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.YearMonthIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.SignStyle
import java.time.YearMonth as jtYearMonth

@Serializable(with = YearMonthIso8601Serializer::class)
public actual class YearMonth internal constructor(
    internal val value: jtYearMonth
) : Comparable<YearMonth>, java.io.Serializable {
    public actual val year: Int get() = value.year
    internal actual val monthNumber: Int get() = value.monthValue

    public actual val month: Month get() = value.month.toKotlinMonth()
    public actual val firstDay: LocalDate get() = LocalDate(value.atDay(1))
    public actual val lastDay: LocalDate get() = LocalDate(value.atEndOfMonth())
    public actual val numberOfDays: Int get() = value.lengthOfMonth()
    public actual val days: LocalDateRange get() = firstDay..lastDay // no ranges yet

    public actual constructor(year: Int, month: Int): this(try {
        jtYearMonth.of(year, month)
    } catch (e: DateTimeException) {
        throw IllegalArgumentException(e)
    })
    public actual constructor(year: Int, month: Month): this(try {
        jtYearMonth.of(year, month.toJavaMonth())
    } catch (e: DateTimeException) {
        throw IllegalArgumentException(e)
    })

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<YearMonth>): YearMonth =
            if (format === Formats.ISO) {
                try {
                    val sanitizedInput = removeLeadingZerosFromLongYearFormYearMonth(input.toString())
                    jtYearMonth.parse(sanitizedInput).let(::YearMonth)
                } catch (e: DateTimeParseException) {
                    throw DateTimeFormatException(e)
                }
            } else {
                format.parse(input)
            }

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithYearMonth.() -> Unit): DateTimeFormat<YearMonth> =
            YearMonthFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<YearMonth> get() = ISO_YEAR_MONTH
    }

    public actual operator fun rangeTo(that: YearMonth): YearMonthRange = YearMonthRange.fromRangeTo(this, that)

    public actual operator fun rangeUntil(that: YearMonth): YearMonthRange = YearMonthRange.fromRangeUntil(this, that)

    actual override fun compareTo(other: YearMonth): Int = value.compareTo(other.value)

    actual override fun toString(): String = isoFormat.format(value)

    override fun equals(other: Any?): Boolean = this === other || other is YearMonth && value == other.value

    override fun hashCode(): Int = value.hashCode()

    private fun writeReplace(): Any = Ser(Ser.YEAR_MONTH_TAG, this)
}

internal fun YearMonth.toEpochMonths(): Long = (year - 1970L) * 12 + monthNumber - 1

internal fun YearMonth.Companion.fromEpochMonths(months: Long): YearMonth {
    val year = months.floorDiv(12) + 1970
    val month = months.mod(12) + 1
    return YearMonth(year.toInt(), month)
}

private val isoFormat by lazy {
    DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendValue(java.time.temporal.ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(java.time.temporal.ChronoField.MONTH_OF_YEAR, 2)
        .toFormatter()
}
