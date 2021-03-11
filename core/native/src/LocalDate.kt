/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.math.*

actual object LocalDateLongSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate.ofEpochDay(decoder.decodeLong().clampToInt())

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeLong(value.toEpochDay().toLong())
    }

}

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatter#ISO_LOCAL_DATE
internal val localDateParser: Parser<LocalDate>
    get() = intParser(4, 10, SignStyle.EXCEEDS_PAD)
        .chainIgnoring(concreteCharParser('-'))
        .chain(intParser(2, 2))
        .chainIgnoring(concreteCharParser('-'))
        .chain(intParser(2, 2))
        .map {
            val (yearMonth, day) = it
            val (year, month) = yearMonth
            try {
                LocalDate(year, month, day)
            } catch (e: IllegalArgumentException) {
                throw DateTimeFormatException(e)
            }
        }

internal const val YEAR_MIN = -999_999
internal const val YEAR_MAX = 999_999

private fun isValidYear(year: Int): Boolean =
    year >= YEAR_MIN && year <= YEAR_MAX

public actual class LocalDate actual constructor(actual val year: Int, actual val monthNumber: Int, actual val dayOfMonth: Int) : Comparable<LocalDate> {

    init {
        // org.threeten.bp.LocalDate#create
        require(isValidYear(year)) { "Invalid date: the year is out of range" }
        require(monthNumber >= 1 && monthNumber <= 12) { "Invalid date: month must be a number between 1 and 12, got $monthNumber" }
        require(dayOfMonth >= 1 && dayOfMonth <= 31) { "Invalid date: day of month must be a number between 1 and 31, got $dayOfMonth" }
        if (dayOfMonth > 28 && dayOfMonth > monthNumber.monthLength(isLeapYear(year))) {
            if (dayOfMonth == 29) {
                throw IllegalArgumentException("Invalid date 'February 29' as '$year' is not a leap year")
            } else {
                throw IllegalArgumentException("Invalid date '${month.name} $dayOfMonth'")
            }
        }
    }

    public actual constructor(year: Int, month: Month, dayOfMonth: Int) : this(year, month.number, dayOfMonth)

    actual companion object {
        actual fun parse(isoString: String): LocalDate =
            localDateParser.parse(isoString)

        // org.threeten.bp.LocalDate#toEpochDay
        /**
         * @throws IllegalArgumentException if the result exceeds the boundaries
         */
        internal fun ofEpochDay(epochDay: Int): LocalDate {
            // LocalDate(-999999, 1, 1).toEpochDay(), LocalDate(999999, 12, 31).toEpochDay()
            // Unidiomatic code due to https://github.com/Kotlin/kotlinx-datetime/issues/5
            require(epochDay >= -365961662 && epochDay <= 364522971) {
                "Invalid date: boundaries of LocalDate exceeded"
            }
            var zeroDay = epochDay + DAYS_0000_TO_1970
            // find the march-based year
            zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle

            var adjust = 0
            if (zeroDay < 0) { // adjust negative years to positive for calculation
                val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEst = ((400 * zeroDay.toLong() + 591) / DAYS_PER_CYCLE).toInt()
            var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            if (doyEst < 0) { // fix estimate
                yearEst--
                doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
            }
            yearEst += adjust // reset any negative year

            val marchDoy0 = doyEst

            // convert march-based values back to january-based
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += marchMonth0 / 10

            return LocalDate(yearEst, month, dom)
        }

        internal actual val MIN = LocalDate(YEAR_MIN, 1, 1)
        internal actual val MAX = LocalDate(YEAR_MAX, 12, 31)
    }

    // org.threeten.bp.LocalDate#toEpochDay
    internal fun toEpochDay(): Int {
        val y = year
        val m = monthNumber
        var total = 0
        total += 365 * y
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
        } else {
            total -= y / -4 - y / -100 + y / -400
        }
        total += ((367 * m - 362) / 12)
        total += dayOfMonth - 1
        if (m > 2) {
            total--
            if (!isLeapYear(year)) {
                total--
            }
        }
        return total - DAYS_0000_TO_1970
    }

    // org.threeten.bp.LocalDate#withYear
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     */
    internal fun withYear(newYear: Int): LocalDate =
        if (newYear == year) this else resolvePreviousValid(newYear, monthNumber, dayOfMonth)

    actual val month: Month
        get() = Month(monthNumber)

    // org.threeten.bp.LocalDate#getDayOfWeek
    actual val dayOfWeek: DayOfWeek
        get() {
            val dow0 = floorMod(toEpochDay() + 3, 7)
            return DayOfWeek(dow0 + 1)
        }

    // org.threeten.bp.LocalDate#getDayOfYear
    actual val dayOfYear: Int
        get() = month.firstDayOfYear(isLeapYear(year)) + dayOfMonth - 1

    // Several times faster than using `compareBy`
    actual override fun compareTo(other: LocalDate): Int {
        val y = year.compareTo(other.year)
        if (y != 0) {
            return y
        }
        val m = monthNumber.compareTo(other.monthNumber)
        if (m != 0) {
            return m
        }
        return dayOfMonth.compareTo(other.dayOfMonth)
    }

    // org.threeten.bp.LocalDate#resolvePreviousValid
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     */
    private fun resolvePreviousValid(year: Int, month: Int, day: Int): LocalDate {
        val newDay = min(day, month.monthLength(isLeapYear(year)))
        return LocalDate(year, month, newDay)
    }

    // org.threeten.bp.LocalDate#plusYears
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusYears(yearsToAdd: Int): LocalDate =
        if (yearsToAdd == 0) this
        else resolvePreviousValid(safeAdd(year, yearsToAdd), monthNumber, dayOfMonth)

    // org.threeten.bp.LocalDate#plusMonths
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusMonths(monthsToAdd: Int): LocalDate {
        if (monthsToAdd == 0) {
            return this
        }
        val monthCount = year * 12 + (monthNumber - 1)
        val calcMonths = safeAdd(monthCount, monthsToAdd)
        val newYear = floorDiv(calcMonths, 12)
        val newMonth = floorMod(calcMonths, 12) + 1
        return resolvePreviousValid(newYear, newMonth, dayOfMonth)
    }

    // org.threeten.bp.LocalDate#plusWeeks
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusWeeks(value: Int): LocalDate =
        plusDays(safeMultiply(value, 7))

    // org.threeten.bp.LocalDate#plusDays
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusDays(daysToAdd: Int): LocalDate =
        if (daysToAdd == 0) this
        else ofEpochDay(safeAdd(toEpochDay(), daysToAdd))

    override fun equals(other: Any?): Boolean =
        this === other || (other is LocalDate && compareTo(other) == 0)

    // org.threeten.bp.LocalDate#hashCode
    override fun hashCode(): Int {
        val yearValue = year
        val monthValue: Int = monthNumber
        val dayValue: Int = dayOfMonth
        return yearValue and -0x800 xor (yearValue shl 11) + (monthValue shl 6) + dayValue
    }

    // org.threeten.bp.LocalDate#toString
    actual override fun toString(): String {
        val yearValue = year
        val monthValue: Int = monthNumber
        val dayValue: Int = dayOfMonth
        val absYear: Int = abs(yearValue)
        val buf = StringBuilder(10)
        if (absYear < 1000) {
            if (yearValue < 0) {
                buf.append(yearValue - 10000).deleteAt(1)
            } else {
                buf.append(yearValue + 10000).deleteAt(0)
            }
        } else {
            if (yearValue > 9999) {
                buf.append('+')
            }
            buf.append(yearValue)
        }
        return buf.append(if (monthValue < 10) "-0" else "-")
            .append(monthValue)
            .append(if (dayValue < 10) "-0" else "-")
            .append(dayValue)
            .toString()
    }
}

public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(1, unit)

public actual fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
    try {
        when (unit) {
            is DateTimeUnit.DateBased.DayBased -> plusDays(safeMultiply(value, unit.days))
            is DateTimeUnit.DateBased.MonthBased -> plusMonths(safeMultiply(value, unit.months))
        }
    } catch (e: ArithmeticException) {
        throw DateTimeArithmeticException("Arithmetic overflow when adding a value to a date", e)
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Boundaries of LocalDate exceeded when adding a value", e)
    }

public actual fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(-value, unit)

public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate =
        if (value > Int.MAX_VALUE || value < Int.MIN_VALUE)
            throw DateTimeArithmeticException("Can't add a Long to a LocalDate") // TODO: less specific message
        else plus(value.toInt(), unit)

actual operator fun LocalDate.plus(period: DatePeriod): LocalDate =
    with(period) {
        try {
            this@plus
                .run { if (totalMonths != 0) plusMonths(totalMonths) else this }
                .run { if (days != 0) plusDays(days) else this }
        } catch (e: ArithmeticException) {
            throw DateTimeArithmeticException("Arithmetic overflow when adding a period to a date", e)
        } catch (e: IllegalArgumentException) {
            throw DateTimeArithmeticException("Boundaries of LocalDate exceeded when adding a period", e)
        }
    }

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.DateBased.MonthBased -> monthsUntil(other) / unit.months
    is DateTimeUnit.DateBased.DayBased -> daysUntil(other) / unit.days
}

// org.threeten.bp.LocalDate#daysUntil
public actual fun LocalDate.daysUntil(other: LocalDate): Int =
    other.toEpochDay() - this.toEpochDay()

// org.threeten.bp.LocalDate#getProlepticMonth
internal val LocalDate.prolepticMonth get() = (year * 12) + (monthNumber - 1)

// org.threeten.bp.LocalDate#monthsUntil
public actual fun LocalDate.monthsUntil(other: LocalDate): Int {
    val packed1 = prolepticMonth * 32 + dayOfMonth
    val packed2 = other.prolepticMonth * 32 + other.dayOfMonth
    return (packed2 - packed1) / 32
}

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
    monthsUntil(other) / 12

actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    val months = monthsUntil(other)
    val days = plusMonths(months).daysUntil(other)
    return DatePeriod(totalMonths = months, days)
}
