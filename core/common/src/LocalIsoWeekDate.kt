/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.removeLeadingZerosFromLongYearFormIsoWeekDate
import kotlin.math.absoluteValue

/**
 * The date part of a [LocalDateTime], represented using the ISO *week* calendar.
 *
 * This class is similar to the far more commonly used [LocalDate], but uses a different calendar system.
 * Here, every year begins on a Monday, and instead of the year being split into 12 months, each with 28-31 days,
 * the week number and the day of the week are used to identify a specific date within the year.
 *
 * More specifically, a [LocalIsoWeekDate] consists of three components:
 * - A week year. In *most* cases, it is equal to the commonly used [ISO year][LocalDate.year].
 *   However, the beginning and the end of the year can be different from [LocalDate.year] by several days,
 *   to ensure the week-year begins on a Monday.
 * - A week number. The number of the week in the given year, starting with 1.
 *   Depending on the year, there can be at most 52 or 53 weeks.
 * - The day of the week, from [DayOfWeek.MONDAY] to [DayOfWeek.SUNDAY].
 *
 * The range of supported dates is exactly the same as that of [LocalDate].
 * It is at least enough to represent dates of all instants between
 * [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE].
 *
 * ### Construction, serialization, and deserialization
 *
 * [LocalIsoWeekDate] can be constructed directly from its components using the constructor.
 * See sample 1.
 *
 * [toLocalDate] and [LocalDate.toLocalIsoWeekDate] can be used to convert between [LocalDate] and [LocalIsoWeekDate].
 * See sample 2.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalIsoWeekDate] from and convert it to a string in the
 * ISO 8601 extended format (for example, `2020-W01-1`).
 * See sample 3.
 *
 * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.constructorFunction
 * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.localDateConversion
 * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.parsingAndFormatting
 */
public class LocalIsoWeekDate(
    /**
     * The week-year part of the week-date. Not to be confused with the [LocalDate.year]!
     *
     * In most scenarios, this number is the same as the commonly used [ISO year][LocalDate.year].
     * However, the beginning and the end of the year can be different from [LocalDate.year] by several days,
     * to ensure the week-year begins on a Monday.
     *
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.weekYearProperty
     */
    public val isoWeekYear: Int,

    /**
     * The ISO week number within the week year.
     *
     * Note that this value is unsuitable for finding the week number within the calendar year,
     * as the normal and the ISO week years can be different for the same date.
     * For example, `2010-01-01` has the [isoWeekNumber] equal to 53, since the corresponding ISO week date is
     * `2009-W53-5`.
     *
     * Has the range of 1 to 52 or 53, depending on the year.
     *
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.weekNumberProperty
     */
    public val isoWeekNumber: Int,

    /**
     * The day of the week. Equals `toLocalDate().dayOfWeek`.
     *
     * Each ISO week starts on [DayOfWeek.MONDAY].
     *
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.dayOfWeekProperty
     */
    public val dayOfWeek: DayOfWeek,
) : Comparable<LocalIsoWeekDate> {

    init {
        when (isoWeekYear) {
            // The happy case: a year with the full range of dates.
            in YEAR_MIN..<YEAR_MAX -> {
                // No extra checks needed
                require(isoWeekNumber in 1..53) { "isoWeekNumber must be in the range 1..53, got: $isoWeekNumber" }
                if (isoWeekNumber == 53) {
                    require(isIsoWeekLeapYear(isoWeekYear)) {
                        "There are only 52 weeks in ISO week year $isoWeekYear, but `isoWeekNumber` is 53"
                    }
                }
            }
            // Max date is +999999999-W52-5
            YEAR_MAX -> {
                require(isoWeekNumber in 1..52) {
                    "isoWeekNumber for year $YEAR_MAX must be in the range 1..52, got: $isoWeekNumber"
                }
                require(isoWeekNumber != 52 || dayOfWeek <= DayOfWeek.FRIDAY) {
                    "In the year $YEAR_MAX, the 52nd week of the year is only representable up to Friday " +
                            "for compatibility with the LocalDate range, but got $dayOfWeek"
                }
            }
            // Simply out of the supported range for `isoWeekYear`.
            else -> throw IllegalArgumentException("The ISO week-year $isoWeekYear is out of range")
        }
    }

    /**
     * Constructs a [LocalIsoWeekDate] instance from the given date components.
     *
     * The components [isoWeekNumber] and [dayOfWeek] are 1-based.
     *
     * The supported ranges of components:
     * - [isoWeekYear] the range is at least enough to represent dates of all instants between
     *   [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [isoWeekNumber] `1..52` or `1..53`, depending on the [isoWeekYear]
     * - [dayOfWeek] `1..7`
     *
     * Additionally, the full range of supported dates is exactly the same as that of [LocalDate].
     *
     * @throws IllegalArgumentException if any parameter is out of range
     * or if [isoWeekNumber] is invalid for the given [isoWeekYear].
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.constructorFunctionDayOfWeekNumber
     */
    public constructor(isoWeekYear: Int, isoWeekNumber: Int, dayOfWeek: Int) : this(
        isoWeekYear,
        isoWeekNumber,
        DayOfWeek(dayOfWeek)
    )

    public companion object {
        /**
         * Parses an ISO 8601 week date string as a [LocalIsoWeekDate].
         *
         * Examples of week dates in the ISO 8601 format:
         *
         * - `2004-W53-6`, the date `2005-01-01`, Saturday
         * - `2004-W53-7`, the date `2005-01-02`, Sunday
         * - `2005-W52-6`, the date `2005-12-31`, Saturday
         * - `2005-W52-7`, the date `2006-01-01`, Sunday
         * - `2006-W01-1`, the date `2006-01-02`, Monday
         * - `2009-W01-2`, the date `2008-12-30`, Tuesday
         * - `+12345-W05-2`
         * - `-0015-W01-2`
         *
         * See ISO-8601-1:2019, 5.2.4.1b), using the "expanded calendar year" extension from 5.2.4.3a), generalized
         * to any number of digits in the year for years that fit in an [Int].
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalIsoWeekDate] are
         * exceeded.
         * @see toString for the dual operation: obtaining a string from a [LocalIsoWeekDate].
         * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.parsing
         */
        public fun parse(isoString: String): LocalIsoWeekDate {
            val sanitizedIsoString = removeLeadingZerosFromLongYearFormIsoWeekDate(isoString)
            fun parseFailure(error: String): Nothing {
                throw DateTimeFormatException("$error when parsing a LocalIsoWeekDate from \"$sanitizedIsoString\"")
            }

            val s = sanitizedIsoString

            fun expect(what: String, where: Int, predicate: (Char) -> Boolean) {
                val c = s[where]
                if (!predicate(c)) {
                    parseFailure("Expected $what, but got '$c' at position $where")
                }
            }
            var i = 0
            require(s.isNotEmpty()) { "An empty string is not a valid LocalIsoWeekDate" }
            val yearSign = when (val c = s[i]) {
                '+', '-' -> {
                    ++i; c
                }

                else -> ' '
            }
            val yearStart = i
            var absYear = 0
            while (i < s.length && s[i] in '0'..'9') {
                absYear = absYear * 10 + (s[i] - '0')
                ++i
            }
            val yearStrLength = i - yearStart
            val year = when {
                yearStrLength > 10 -> {
                    parseFailure("Expected at most 10 digits for the year number, got $yearStrLength digits")
                }

                yearStrLength == 10 && s[yearStart] >= '2' -> {
                    parseFailure("Expected at most 9 digits for the year number or year 1000000000, got $yearStrLength digits")
                }

                yearStrLength < 4 -> {
                    parseFailure("The year number must be padded to 4 digits, got $yearStrLength digits")
                }

                else -> {
                    if (yearSign == '+' && yearStrLength == 4) {
                        parseFailure("The '+' sign at the start is only valid for year numbers longer than 4 digits")
                    }
                    if (yearSign == ' ' && yearStrLength != 4) {
                        parseFailure("A '+' or '-' sign is required for year numbers longer than 4 digits")
                    }
                    if (yearSign == '-') -absYear else absYear
                }
            }
            // reading exactly -Www-D
            //                 012345 6 chars
            if (s.length < i + 6) {
                parseFailure("The input string is too short")
            }
            expect("'-'", i) { it == '-' }
            expect("'W'", i + 1) { it == 'W' || it == 'w' }
            expect("'-'", i + 4) { it == '-' }
            for (j in listOf(2, 3, 5)) {
                expect("an ASCII digit", i + j) { it in '0'..'9' }
            }
            val weekNumber = (s[i + 2] - '0') * 10 + (s[i + 3] - '0')
            val isoDayOfWeekNumber = (s[i + 5] - '0')
            val dayOfWeek = DayOfWeek(isoDayOfWeekNumber)
            if (s.length > i + 6) {
                parseFailure("Trailing characters")
            }
            try {
                return LocalIsoWeekDate(year, weekNumber, dayOfWeek)
            } catch (e: IllegalArgumentException) {
                throw DateTimeFormatException("Invalid ISO week date: $isoString", e)
            }
        }

        /**
         * Parses an ISO 8601 week date string as a [LocalIsoWeekDate] or returns `null` if the string could not be
         * parsed into a [LocalIsoWeekDate].
         *
         * See [parse] for the list of supported formats.
         *
         * @see parse for a version of this function that throws an exception on faulty input.
         * @see toString for the dual operation: obtaining a string from a [LocalIsoWeekDate].
         * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.parseOrNull
         */
        public fun parseOrNull(isoString: String): LocalIsoWeekDate? = try {
            parse(isoString)
        } catch (_: DateTimeFormatException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Converts this [LocalIsoWeekDate] to a [LocalDate].
     *
     * [LocalIsoWeekDate] values are in one-to-one correspondence with [LocalDate] values, so this method never throws.
     *
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.localDateConversion
     */
    public fun toLocalDate(): LocalDate =
        startOfIsoWeekYear(isoWeekYear)
            .plus(isoWeekNumber - 1, DateTimeUnit.WEEK)
            .plus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    /**
     * Converts this [LocalIsoWeekDate] to a string in the ISO 8601 week date format (for example, `2023-W42-3`).
     *
     * See ISO-8601-1:2019, 5.2.4.1b), using the "expanded calendar year" extension from 5.2.4.3a), generalized
     * to any number of digits in the year for years that fit in an [Int].
     *
     * @see parse for the dual operation: obtaining [LocalIsoWeekDate] from a string.
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.toStringSample
     */
    override fun toString(): String = buildString {
        fun Appendable.appendTwoDigits(number: Int) {
            if (number < 10) append('0')
            append(number)
        }
        run {
            val number = isoWeekYear
            when {
                number.absoluteValue < 1_000 -> {
                    val innerBuilder = StringBuilder()
                    if (number >= 0) {
                        innerBuilder.append((number + 10_000)).deleteAt(0)
                    } else {
                        innerBuilder.append((number - 10_000)).deleteAt(1)
                    }
                    append(innerBuilder)
                }

                else -> {
                    if (number >= 10_000) append('+')
                    append(number)
                }
            }
        }
        append("-W")
        appendTwoDigits(isoWeekNumber)
        append('-')
        append(dayOfWeek.isoDayNumber)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is LocalIsoWeekDate &&
                        this.isoWeekYear == other.isoWeekYear &&
                        this.isoWeekNumber == other.isoWeekNumber &&
                        this.dayOfWeek == other.dayOfWeek)

    override fun hashCode(): Int = isoWeekYear xor isoWeekNumber xor dayOfWeek.hashCode()

    /**
     * Compares this [LocalIsoWeekDate] to another one.
     * Returns zero if the two [LocalIsoWeekDate]s represent the same date,
     * a negative number if this date is earlier than the [other],
     * and a positive number if this date is later than the [other].
     *
     * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.compareToSample
     */
    override fun compareTo(other: LocalIsoWeekDate): Int =
        compareValuesBy(this, other, { it.isoWeekYear }, { it.isoWeekNumber }, { it.dayOfWeek })
}

/**
 * Converts this [LocalDate] to a [LocalIsoWeekDate].
 *
 * [LocalIsoWeekDate] values are in one-to-one correspondence with [LocalDate] values, so this method never throws.
 *
 * @sample kotlinx.datetime.test.samples.LocalIsoWeekDateSamples.localDateConversion
 */
public fun LocalDate.toLocalIsoWeekDate(): LocalIsoWeekDate {
    // The ISO week year differs from the normal ISO year by at most +/- 1
    val isoWeekYear: Int
    val beginningOfIsoWeekYear: LocalDate = run {
        val thisYear = startOfIsoWeekYear(year)
        if (this < thisYear) {
            isoWeekYear = year - 1
            startOfIsoWeekYear(isoWeekYear)
        } else {
            val nextYear = startOfIsoWeekYear(year + 1)
            if (this >= nextYear) {
                isoWeekYear = year + 1
                nextYear
            } else {
                isoWeekYear = year
                thisYear
            }
        }
    }
    val zeroBasedWeekNumber = beginningOfIsoWeekYear.until(this, DateTimeUnit.WEEK)
    return LocalIsoWeekDate(
        isoWeekYear = isoWeekYear,
        isoWeekNumber = zeroBasedWeekNumber.toInt() + 1,
        dayOfWeek = this.dayOfWeek,
    )
}

private fun startOfIsoWeekYear(weekYear: Int): LocalDate =
    LocalDate(weekYear, 1, 4).previousOrSame(DayOfWeek.MONDAY)

// https://en.wikipedia.org/wiki/ISO_week_date#Weeks_per_year
private fun isIsoWeekLeapYear(weekYear: Int): Boolean {
    // `y` is approx. -10^9..10^9, so the intermediate values below fit into an Int
    fun p(y: Int) = (y + y.floorDiv(4) - y.floorDiv(100) + y.floorDiv(400)).mod(7)
    return p(weekYear) == 4 || p(weekYear - 1) == 3
}
