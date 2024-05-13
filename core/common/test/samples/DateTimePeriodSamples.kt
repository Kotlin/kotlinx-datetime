/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class DateTimePeriodSamples {

    @Test
    fun construction() {
        // Constructing a DateTimePeriod using its constructor function
        val period = DateTimePeriod(years = 5, months = 21, days = 36, seconds = 3601)
        check(period.years == 6) // 5 years + (21 months / 12)
        check(period.months == 9) // 21 months % 12
        check(period.days == 36)
        check(period.hours == 1) // 3601 seconds / 3600
        check(period.minutes == 0)
        check(period.seconds == 1)
        check(period.nanoseconds == 0)
        check(DateTimePeriod(months = -24) as DatePeriod == DatePeriod(years = -2))
    }

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing and formatting a DateTimePeriod
        val string = "P-2M-3DT-4H60M"
        val period = DateTimePeriod.parse(string)
        check(period.toString() == "-P2M3DT3H")
    }

    @Test
    fun valueNormalization() {
        // Reading the normalized values that make up a DateTimePeriod
        val period = DateTimePeriod(
            years = -12, months = 122, days = -1440,
            hours = 400, minutes = -80, seconds = 123, nanoseconds = -123456789
        )
        // years and months have the same sign and are normalized together:
        check(period.years == -1) // -12 years + (122 months % 12) + 1 year
        check(period.months == -10) // (122 months % 12) - 1 year
        // days are separate from months and are not normalized:
        check(period.days == -1440)
        // hours, minutes, seconds, and nanoseconds are normalized together and have the same sign:
        check(period.hours == 398) // 400 hours - 2 hours' worth of minutes
        check(period.minutes == 42) // -80 minutes + 2 hours' worth of minutes + 120 seconds
        check(period.seconds == 2) // 123 seconds - 2 minutes' worth of seconds - 1 second
        check(period.nanoseconds == 876543211) // -123456789 nanoseconds + 1 second
    }

    @Test
    fun toStringSample() {
        // Formatting a DateTimePeriod to a string
        check(DateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5, seconds = 6, nanoseconds = 7).toString() == "P1Y2M3DT4H5M6.000000007S")
        check(DateTimePeriod(months = 14, days = -16, hours = 5).toString() == "P1Y2M-16DT5H")
        check(DateTimePeriod(months = -2, days = -16, hours = -5).toString() == "-P2M16DT5H")
    }

    @Test
    fun parsing() {
        // Parsing a string representation of a DateTimePeriod
        with(DateTimePeriod.parse("P1Y2M3DT4H5M6.000000007S")) {
            check(years == 1)
            check(months == 2)
            check(days == 3)
            check(hours == 4)
            check(minutes == 5)
            check(seconds == 6)
            check(nanoseconds == 7)
        }
        with(DateTimePeriod.parse("P14M-16DT5H")) {
            check(years == 1)
            check(months == 2)
            check(days == -16)
            check(hours == 5)
        }
        with(DateTimePeriod.parse("-P2M16DT5H")) {
            check(years == 0)
            check(months == -2)
            check(days == -16)
            check(hours == -5)
        }
    }

    @Test
    fun constructorFunction() {
        // Constructing a DateTimePeriod using its constructor function
        val dateTimePeriod = DateTimePeriod(months = 16, days = -60, hours = 16, minutes = -61)
        check(dateTimePeriod.years == 1) // months overflowed to years
        check(dateTimePeriod.months == 4) // 16 months % 12
        check(dateTimePeriod.days == -60) // days are separate from months and are not normalized
        check(dateTimePeriod.hours == 14) // the negative minutes overflowed to hours
        check(dateTimePeriod.minutes == 59) // (-61 minutes) + (2 hours) * (60 minutes / hour)

        val datePeriod = DateTimePeriod(months = 15, days = 3, hours = 2, minutes = -120)
        check(datePeriod is DatePeriod) // the time components are zero
    }

    @Test
    fun durationToDateTimePeriod() {
        // Converting a Duration to a DateTimePeriod that only has time-based components
        check(130.minutes.toDateTimePeriod() == DateTimePeriod(minutes = 130))
        check(2.days.toDateTimePeriod() == DateTimePeriod(days = 0, hours = 48))
    }
}

class DatePeriodSamples {

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing and formatting a DatePeriod
        val datePeriod1 = DatePeriod(years = 1, days = 3)
        val string = datePeriod1.toString()
        check(string == "P1Y3D")
        val datePeriod2 = DatePeriod.parse(string)
        check(datePeriod1 == datePeriod2)
    }

    @Test
    fun construction() {
        // Constructing a DatePeriod using its constructor
        val datePeriod = DatePeriod(years = 1, months = 16, days = 60)
        check(datePeriod.years == 2) // 1 year + (16 months / 12)
        check(datePeriod.months == 4) // 16 months % 12
        check(datePeriod.days == 60)
        // the time components are always zero:
        check(datePeriod.hours == 0)
        check(datePeriod.minutes == 0)
        check(datePeriod.seconds == 0)
        check(datePeriod.nanoseconds == 0)
    }

    @Test
    fun parsing() {
        // Parsing a string representation of a DatePeriod
        // ISO duration strings are supported:
        val datePeriod = DatePeriod.parse("P1Y16M60D")
        check(datePeriod == DatePeriod(years = 2, months = 4, days = 60))
        // it's okay to have time components as long as they amount to zero in total:
        val datePeriodWithTimeComponents = DatePeriod.parse("P1Y2M3DT1H-60M")
        check(datePeriodWithTimeComponents == DatePeriod(years = 1, months = 2, days = 3))
    }
}
