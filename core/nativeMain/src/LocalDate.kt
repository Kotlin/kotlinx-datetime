/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.*

public actual class LocalDate internal constructor (actual val year: Int, actual val month: Month, actual val dayOfMonth: Int) : Comparable<LocalDate> {
    actual companion object {
        actual fun parse(isoString: String): LocalDate {
            TODO("Not yet implemented")
        }
    }

    actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) : this(year, Month(monthNumber), dayOfMonth)

    private fun toEpochDay(): Long {
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

    actual val monthNumber: Int = month.number

    actual val dayOfWeek: DayOfWeek
        get() {
            val dow0 = ((toEpochDay() + 3) / 7).toInt()
            return DayOfWeek(dow0 + 1)
        }

    actual val dayOfYear: Int
        get() = month.firstDayOfYear(isLeapYear(year)) + dayOfMonth - 1

    actual override fun compareTo(other: LocalDate): Int =
        compareBy<LocalDate>({ it.year }, { it.monthNumber }, {it.dayOfMonth}).compare(this, other)

    private fun resolvePreviousValid(year: Int, month: Month, day: Int): LocalDate {
        val newDay = when (month) {
            Month.FEBRUARY -> min(day, if (isLeapYear(year)) 29 else 28)
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> min(day, 30)
            else -> day
        }
        return LocalDate(year, month, newDay)
    }

    public fun plusYears(yearsToAdd: Long): LocalDate =
        if (yearsToAdd == 0L) {
            this
        } else {
            val newYear = safeAdd(year.toLong(), yearsToAdd).toInt()
            resolvePreviousValid(newYear, month, dayOfMonth)
        }

    public fun plusMonths(monthsToAdd: Long): LocalDate {
        if (monthsToAdd == 0L) {
            return this
        }
        val monthCount: Long = year * 12L + (monthNumber - 1)
        val calcMonths = monthCount + monthsToAdd // safe overflow
        val newYear: Int = /* YEAR.checkValidIntValue( */ floorDiv(calcMonths, 12).toInt()
        val newMonth = Month(floorMod(calcMonths, 12).toInt() + 1)
        return resolvePreviousValid(newYear, newMonth, dayOfMonth)
    }

    public fun plusWeeks(value: Long): LocalDate =
        plusDays(safeMultiply(value, 7))

    fun plusDays(daysToAdd: Long): LocalDate {
        if (daysToAdd == 0L) {
            return this
        }
        val mjDay: Long = safeAdd(toEpochDay(), daysToAdd)
        return ofEpochDay(mjDay)
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

actual operator fun LocalDate.plus(period: CalendarPeriod): LocalDate {
    TODO("Not yet implemented")
}
