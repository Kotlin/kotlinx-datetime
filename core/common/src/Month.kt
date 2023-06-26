/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.native.concurrent.*

/**
 * The enumeration class representing the 12 months of the year.
 */
public expect enum class Month {
    /** January, month #01, with 31 days. */
    JANUARY,

    /** February, month #02, with 28 days, or 29 in leap years. */
    FEBRUARY,

    /** March, month #03, with 31 days. */
    MARCH,

    /** April, month #04, with 30 days. */
    APRIL,

    /** May, month #05, with 31 days. */
    MAY,

    /** June, month #06, with 30 days. */
    JUNE,

    /** July, month #07, with 31 days. */
    JULY,

    /** August, month #08, with 31 days. */
    AUGUST,

    /** September, month #09, with 30 days. */
    SEPTEMBER,

    /** October, month #10, with 31 days. */
    OCTOBER,

    /** November, month #11, with 30 days. */
    NOVEMBER,

    /** December, month #12, with 31 days. */
    DECEMBER;

//    val value: Int // member missing in java.time.Month has to be an extension
}

/**
 * The number of the [Month]. January is 1, December is 12.
 */
public val Month.number: Int get() = ordinal + 1

private val allMonths = Month.values().asList()

/**
 * Returns the [Month] instance for the given month number. January is 1, December is 12.
 */
public fun Month(number: Int): Month {
    require(number in 1..12)
    return allMonths[number - 1]
}


// companion object members vs type aliasing to java.time.Month?
