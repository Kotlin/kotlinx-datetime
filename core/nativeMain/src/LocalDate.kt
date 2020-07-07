/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.math.*

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
            LocalDate(year, month, day)
        }

public actual class LocalDate actual constructor(actual val year: Int, actual val monthNumber: Int, actual val dayOfMonth: Int) : Comparable<LocalDate> {

    init {
        // org.threeten.bp.LocalDate#create
        require(year >= -999_999_999 && year <= 999_999_999) { "Invalid date: the year is out of range" }
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

    actual companion object {
        actual fun parse(isoString: String): LocalDate =
            localDateParser.parse(isoString)

        // org.threeten.bp.LocalDate#toEpochDay
        internal fun ofEpochDay(epochDay: Long): LocalDate {
            // Unidiomatic code due to https://github.com/Kotlin/kotlinx-datetime/issues/5
            require(epochDay >= -365243219162L && epochDay <= 365241780471L)
            var zeroDay: Long = epochDay + DAYS_0000_TO_1970
            // find the march-based year
            zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle

            var adjust: Long = 0
            if (zeroDay < 0) { // adjust negative years to positive for calculation
                val adjustCycles: Long = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                adjust = adjustCycles * 400
                zeroDay += -adjustCycles * DAYS_PER_CYCLE
            }
            var yearEst: Long = (400 * zeroDay + 591) / DAYS_PER_CYCLE
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
            yearEst += marchMonth0 / 10.toLong()

            // check year now we are certain it is correct
            return LocalDate(yearEst.toInt(), month, dom)
        }
    }

    // org.threeten.bp.LocalDate#toEpochDay
    internal fun toEpochDay(): Long {
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

    internal fun withYear(newYear: Int): LocalDate = LocalDate(newYear, monthNumber, dayOfMonth)

    actual val month: Month
        get() = Month(monthNumber)

    // org.threeten.bp.LocalDate#getDayOfWeek
    actual val dayOfWeek: DayOfWeek
        get() {
            val dow0 = floorMod(toEpochDay() + 3, 7).toInt()
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
        return dayOfMonth.compareTo(dayOfMonth)
    }

    // org.threeten.bp.LocalDate#resolvePreviousValid
    private fun resolvePreviousValid(year: Int, month: Int, day: Int): LocalDate {
        val newDay = min(day, month.monthLength(isLeapYear(year)))
        return LocalDate(year, month, newDay)
    }

    // org.threeten.bp.LocalDate#plusYears
    internal fun plusYears(yearsToAdd: Long): LocalDate =
        if (yearsToAdd == 0L) {
            this
        } else {
            val newYear = safeAdd(year.toLong(), yearsToAdd).toInt()
            resolvePreviousValid(newYear, monthNumber, dayOfMonth)
        }

    // org.threeten.bp.LocalDate#plusMonths
    internal fun plusMonths(monthsToAdd: Long): LocalDate {
        if (monthsToAdd == 0L) {
            return this
        }
        val monthCount: Long = year * 12L + (monthNumber - 1)
        val calcMonths = monthCount + monthsToAdd // safe overflow
        val newYear: Int = /* YEAR.checkValidIntValue( */ floorDiv(calcMonths, 12).toInt()
        val newMonth = floorMod(calcMonths, 12).toInt() + 1
        return resolvePreviousValid(newYear, newMonth, dayOfMonth)
    }

    // org.threeten.bp.LocalDate#plusWeeks
    internal fun plusWeeks(value: Long): LocalDate =
        plusDays(safeMultiply(value, 7))

    // org.threeten.bp.LocalDate#plusDays
    internal fun plusDays(daysToAdd: Long): LocalDate {
        if (daysToAdd == 0L) {
            return this
        }
        val mjDay: Long = safeAdd(toEpochDay(), daysToAdd)
        return ofEpochDay(mjDay)
    }

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
    override fun toString(): String {
        val yearValue = year
        val monthValue: Int = monthNumber
        val dayValue: Int = dayOfMonth
        val absYear: Int = abs(yearValue)
        val buf = StringBuilder(10)
        if (absYear < 1000) {
            if (yearValue < 0) {
                buf.append(yearValue - 10000).deleteCharAt(1)
            } else {
                buf.append(yearValue + 10000).deleteCharAt(0)
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

actual fun LocalDate.plus(value: Long, unit: CalendarUnit): LocalDate =
    when (unit) {
        CalendarUnit.YEAR -> plusYears(value)
        CalendarUnit.MONTH -> plusMonths(value)
        CalendarUnit.WEEK -> plusWeeks(value)
        CalendarUnit.DAY -> plusDays(value)
        CalendarUnit.HOUR,
        CalendarUnit.MINUTE,
        CalendarUnit.SECOND,
        CalendarUnit.NANOSECOND -> throw UnsupportedOperationException("Only date based units can be added to LocalDate")
    }

actual fun LocalDate.plus(value: Int, unit: CalendarUnit): LocalDate =
    plus(value.toLong(), unit)

actual operator fun LocalDate.plus(period: CalendarPeriod): LocalDate =
    with(period) {
        if (hours != 0 || minutes != 0 || seconds != 0L || nanoseconds != 0L) {
            throw UnsupportedOperationException("Only date based units can be added to LocalDate")
        }

        return@with this@plus
            .run { if (years != 0 && months == 0) plusYears(years.toLong()) else this }
            .run { if (months != 0) this.plusMonths(years * 12L + months.toLong()) else this }
            .run { if (days != 0) this.plusDays(days.toLong()) else this }
    }

// org.threeten.bp.LocalDate#daysUntil
internal fun LocalDate.daysUntil(other: LocalDate): Long =
    other.toEpochDay() - this.toEpochDay()

// org.threeten.bp.LocalDate#getProlepticMonth
internal val LocalDate.prolepticMonth get() = (year * 12L) + (monthNumber - 1)

// org.threeten.bp.LocalDate#monthsUntil
internal fun LocalDate.monthsUntil(other: LocalDate): Long {
    val packed1: Long = prolepticMonth * 32L + dayOfMonth
    val packed2: Long = other.prolepticMonth * 32L + other.dayOfMonth
    return (packed2 - packed1) / 32
}

// org.threeten.bp.LocalDate#until(org.threeten.bp.temporal.Temporal, org.threeten.bp.temporal.TemporalUnit)
internal fun LocalDate.until(end: LocalDate, unit: CalendarUnit): Long =
    when (unit) {
        CalendarUnit.DAY -> daysUntil(end)
        CalendarUnit.WEEK -> daysUntil(end) / 7
        CalendarUnit.MONTH -> monthsUntil(end)
        CalendarUnit.YEAR -> monthsUntil(end) / 12
        CalendarUnit.HOUR,
        CalendarUnit.MINUTE,
        CalendarUnit.SECOND,
        CalendarUnit.NANOSECOND -> throw UnsupportedOperationException("Unsupported unit: $unit")
    }

actual fun LocalDate.periodUntil(other: LocalDate): CalendarPeriod {
    val months = until(other, CalendarUnit.MONTH)
    val days = plusMonths(months).until(other, CalendarUnit.DAY)
    return CalendarPeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt())
}
