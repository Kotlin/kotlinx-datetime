/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect enum class Month {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER;

//    val value: Int // member missing in java.time.Month has to be an extension
}

public val Month.number: Int get() = ordinal + 1

private val allMonths = Month.values().asList()
public fun Month(number: Int): Month {
    require(number in 1..12)
    return allMonths[number - 1]
}


// From threetenbp
public fun Month.firstDayOfYear(leapYear: Boolean): Int {
    val leap = if (leapYear) 1 else 0
    return when (this) {
        Month.JANUARY -> 1
        Month.FEBRUARY -> 32
        Month.MARCH -> 60 + leap
        Month.APRIL -> 91 + leap
        Month.MAY -> 121 + leap
        Month.JUNE -> 152 + leap
        Month.JULY -> 182 + leap
        Month.AUGUST -> 213 + leap
        Month.SEPTEMBER -> 244 + leap
        Month.OCTOBER -> 274 + leap
        Month.NOVEMBER -> 305 + leap
        Month.DECEMBER -> 335 + leap
    }
}

public fun Month.length(leapYear: Boolean): Int {
    return when (this) {
        Month.FEBRUARY -> if (leapYear) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
}

// companion object members vs typealiasing to java.time.Month?
