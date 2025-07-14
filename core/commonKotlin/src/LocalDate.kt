/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.math.*

internal const val YEAR_MIN = -999_999_999
internal const val YEAR_MAX = 999_999_999

private fun isValidYear(year: Int): Boolean =
    year >= YEAR_MIN && year <= YEAR_MAX

@Serializable(with = LocalDateSerializer::class)
public actual class LocalDate actual constructor(public actual val year: Int, month: Int, public actual val day: Int) : Comparable<LocalDate> {

    private val _month: Int = month
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public actual val monthNumber: Int get() = _month
    @Deprecated("Use the 'day' property instead", ReplaceWith("this.day"), level = DeprecationLevel.WARNING)
    public actual val dayOfMonth: Int get() = day

    init {
        // org.threeten.bp.LocalDate#create
        require(isValidYear(year)) { "Invalid date: the year is out of range" }
        require(_month in 1..12) { "Invalid date: month must be a number between 1 and 12, got $_month" }
        require(day in 1..31) { "Invalid date: day of month must be a number between 1 and 31, got $day" }
        if (day > 28 && day > _month.monthLength(isLeapYear(year))) {
            if (day == 29) {
                throw IllegalArgumentException("Invalid date 'February 29' as '$year' is not a leap year")
            } else {
                throw IllegalArgumentException("Invalid date '${Month(month)} $day'")
            }
        }
    }

    public actual constructor(year: Int, month: Month, day: Int) : this(year, month.number, day)

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDate>): LocalDate = format.parse(input)

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalDate = parse(input = isoString)

        // org.threeten.bp.LocalDate#toEpochDay
        public actual fun fromEpochDays(epochDays: Long): LocalDate {
            // LocalDate(-999_999_999, 1, 1).toEpochDay(), LocalDate(999_999_999, 12, 31).toEpochDay()
            require(epochDays in MIN_EPOCH_DAY..MAX_EPOCH_DAY) {
                "Invalid date: epoch day $epochDays is outside the boundaries of LocalDate"
            }
            var zeroDay = epochDays + DAYS_0000_TO_1970 // -3.7e11 .. 3.7e11
            // find the march-based year
            zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four-year cycle

            var adjust = 0L // -1e9 .. 0
            if (zeroDay < 0) { // adjust negative years to positive for calculation
                val adjustCycles = ((zeroDay + 1) / DAYS_PER_CYCLE - 1) // -2.5e6 .. -1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
                // zeroDay = DAYS_PER_CYCLE - (-zeroDay - 1) % DAYS_PER_CYCLE - 1, in 0 ..< DAYS_PER_CYCLE
            }
            // zeroDay in 0 .. 3.7e11 now
            var yearEst = ((400 * zeroDay + 591) / DAYS_PER_CYCLE) // -1e9 .. 1e9
            var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            if (doyEst < 0) { // fix estimate
                yearEst--
                doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            }
            yearEst += adjust // reset any negative year

            val marchDoy0 = doyEst.toInt()

            // convert march-based values back to january-based
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += marchMonth0 / 10

            return LocalDate(yearEst.toInt(), month, dom)
        }

        public actual fun fromEpochDays(epochDays: Int): LocalDate = fromEpochDays(epochDays.toLong())

        internal actual val MIN = LocalDate(YEAR_MIN, 1, 1)
        internal actual val MAX = LocalDate(YEAR_MAX, 12, 31)

        internal const val MIN_EPOCH_DAY = -365243219162
        internal const val MAX_EPOCH_DAY = 365241780471

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> =
            LocalDateFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDate> get() = ISO_DATE

        public actual val ISO_BASIC: DateTimeFormat<LocalDate> = ISO_DATE_BASIC
    }

    // org.threeten.bp.LocalDate#toEpochDay
    public actual fun toEpochDays(): Long {
        val y = year.toLong()
        val m = _month.toLong()
        var total = 0L
        total += 365 * y
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
        } else {
            total -= y / -4 - y / -100 + y / -400
        }
        total += ((367 * m - 362) / 12)
        total += day - 1
        if (m > 2) {
            total--
            if (!isLeapYear(year)) {
                total--
            }
        }
        return total - DAYS_0000_TO_1970
    }

    public actual val month: Month
        get() = Month(_month)

    // org.threeten.bp.LocalDate#getDayOfWeek
    public actual val dayOfWeek: DayOfWeek
        get() {
            val dow0 = (toEpochDays() + 3).mod(7)
            return DayOfWeek(dow0 + 1)
        }

    // org.threeten.bp.LocalDate#getDayOfYear
    public actual val dayOfYear: Int
        get() = month.firstDayOfYear(isLeapYear(year)) + day - 1

    // Several times faster than using `compareBy`
    actual override fun compareTo(other: LocalDate): Int {
        val y = year.compareTo(other.year)
        if (y != 0) {
            return y
        }
        val m = _month.compareTo(other._month)
        if (m != 0) {
            return m
        }
        return day.compareTo(other.day)
    }

    // org.threeten.bp.LocalDate#resolvePreviousValid
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     */
    private fun resolvePreviousValid(year: Int, month: Int, day: Int): LocalDate {
        val newDay = min(day, month.monthLength(isLeapYear(year)))
        return LocalDate(year, month, newDay)
    }

    // org.threeten.bp.LocalDate#plusMonths
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusMonths(monthsToAdd: Long): LocalDate {
        if (monthsToAdd == 0L) {
            return this
        }
        val monthCount = year * 12L + (_month - 1)
        val calcMonths = safeAdd(monthCount, monthsToAdd)
        val newYear = calcMonths.floorDiv(12)
        if (newYear !in YEAR_MIN..YEAR_MAX) {
            throw IllegalArgumentException("The result of adding $monthsToAdd months to $this is out of LocalDate range.")
        }
        val newMonth = calcMonths.mod(12) + 1
        return resolvePreviousValid(newYear.toInt(), newMonth, day)
    }

    // org.threeten.bp.LocalDate#plusDays
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusDays(daysToAdd: Long): LocalDate =
        if (daysToAdd == 0L) this
        else fromEpochDays(safeAdd(toEpochDays(), daysToAdd))

    public actual operator fun rangeTo(that: LocalDate): LocalDateRange = LocalDateRange.fromRangeTo(this, that)

    public actual operator fun rangeUntil(that: LocalDate): LocalDateRange = LocalDateRange.fromRangeUntil(this, that)

    override fun equals(other: Any?): Boolean =
        this === other || (other is LocalDate && compareTo(other) == 0)

    // org.threeten.bp.LocalDate#hashCode
    override fun hashCode(): Int {
        val yearValue = year
        val monthValue: Int = _month
        val dayValue: Int = day
        return yearValue and -0x800 xor (yearValue shl 11) + (monthValue shl 6) + dayValue
    }

    // org.threeten.bp.LocalDate#toString
    actual override fun toString(): String = format(Formats.ISO)
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate = plus(1, unit)

public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate = try {
    when (unit) {
        is DateTimeUnit.DayBased -> plusDays(safeMultiply(value, unit.days.toLong()))
        is DateTimeUnit.MonthBased -> plusMonths(safeMultiply(value, unit.months.toLong()))
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding a value to a date", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of LocalDate exceeded when adding a value", e)
}

public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate =
    with(period) {
        try {
            this@plus
                .run { if (totalMonths != 0L) plusMonths(totalMonths) else this }
                .run { if (days != 0) plusDays(days.toLong()) else this }
        } catch (e: ArithmeticException) {
            throw DateTimeArithmeticException("Arithmetic overflow when adding a period to a date", e)
        } catch (e: IllegalArgumentException) {
            throw DateTimeArithmeticException("Boundaries of LocalDate exceeded when adding a period", e)
        }
    }

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Long = when(unit) {
    is DateTimeUnit.MonthBased -> {
        val packed1 = prolepticMonth * 32 + day
        val packed2 = other.prolepticMonth * 32 + other.day
        val result = (packed2 - packed1) / 32
        result / unit.months
    }
    is DateTimeUnit.DayBased -> {
        (other.toEpochDays() - this.toEpochDays()) / unit.days
    }
}

// org.threeten.bp.LocalDate#getProlepticMonth
private val LocalDate.prolepticMonth get() = (year * 12L) + (month.number - 1)

// org.threeten.bp.LocalDate#daysUntil
public actual fun LocalDate.daysUntil(other: LocalDate): Int = until(other, DateTimeUnit.DAY).clampToInt()

// org.threeten.bp.LocalDate#monthsUntil
public actual fun LocalDate.monthsUntil(other: LocalDate): Int = until(other, DateTimeUnit.MONTH).clampToInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int = until(other, DateTimeUnit.YEAR).toInt()

public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    val months = until(other, DateTimeUnit.MONTH)
    val days = plusMonths(months).daysUntil(other)
    return DatePeriod(totalMonths = months, days)
}

internal fun LocalDate.previousOrSame(dayOfWeek: DayOfWeek) =
    minus((this.dayOfWeek.isoDayNumber - dayOfWeek.isoDayNumber).mod(7), DateTimeUnit.DAY)

internal fun LocalDate.nextOrSame(dayOfWeek: DayOfWeek) =
    plus((dayOfWeek.isoDayNumber - this.dayOfWeek.isoDayNumber).mod(7), DateTimeUnit.DAY)
